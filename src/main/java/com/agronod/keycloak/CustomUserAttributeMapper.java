package com.agronod.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.StringUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// To configure this in Terraform.
// https://registry.terraform.io/providers/mrparkers/keycloak/latest/docs/resources/custom_identity_provider_mapper

//https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/broker/saml/mappers/UserAttributeMapper.java
public class CustomUserAttributeMapper extends AbstractIdentityProviderMapper implements SamlMetadataDescriptorUpdater {

    public static final String PROVIDER_ID = "saml-agronod-user-attribute-idp-mapper";

    private static Logger logger = Logger.getLogger(CustomUserAttributeMapper.class);
    private DatabaseAccess databaseAccess;

    public static final String[] COMPATIBLE_PROVIDERS = { SAMLIdentityProviderFactory.PROVIDER_ID };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_FRIENDLY_NAME = "attribute.friendly.name";
    public static final String ATTRIBUTE_NAME_FORMAT = "attribute.name.format";
    public static final String USER_ATTRIBUTE = "user.attribute";
    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String SSN = "ssn";

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(
            Arrays.asList(IdentityProviderSyncMode.values()));

    public static final List<String> NAME_FORMATS = Arrays.asList(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.name(),
            JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.name(),
            JBossSAMLURIConstants.ATTRIBUTE_FORMAT_UNSPECIFIED.name());
    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME);
        property.setLabel("Attribute Name");
        property.setHelpText(
                "Name of attribute to search for in assertion.  You can leave this blank and specify a friendly name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_FRIENDLY_NAME);
        property.setLabel("Friendly Name");
        property.setHelpText(
                "Friendly name of attribute to search for in assertion.  You can leave this blank and specify a name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME_FORMAT);
        property.setLabel("Name Format");
        property.setHelpText(
                "Name format of attribute to specify in the RequestedAttribute element. Default to basic format.");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(NAME_FORMATS);
        property.setDefaultValue(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.name());
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("Agronod Anvandare Attribute Name");
        property.setHelpText(
                "Agronod Anvandare column name to store saml attribute.  Use ssn, email, name to map to Anvandare properties.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName("connectionstring");
        property.setLabel("Database connectionstring");
        property.setHelpText("Connectionstring to database");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName("maxPoolSize");
        property.setLabel("Max db connection pool size");
        property.setHelpText("Max db connection pool size");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("2");
        configProperties.add(property);
    }

    public CustomUserAttributeMapper() {
        this.databaseAccess = new DatabaseAccess();
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Agronod Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Agronod Attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (StringUtil.isNullOrEmpty(attribute)) {
            return;
        }
        String attributeName = getAttributeNameFromMapperModel(mapperModel);

        List<String> attributeValuesInContext = findAttributeValuesInContext(attributeName, context);
        if (!attributeValuesInContext.isEmpty()) {

            String connectionString = mapperModel.getConfig().get("connectionstring");
            String maxPoolSize = mapperModel.getConfig().get("maxPoolSize");
            try {
                // Set correct driver
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
            }

            try (Connection conn = DataSource.getConnection(connectionString, Integer.parseInt(maxPoolSize))) {
                String brokerUserId = context.getId(); // username in Keycloak

                // All this to load the user Id...
                IdentityProviderModel identityProviderConfig = context.getIdpConfig();
                String providerId = identityProviderConfig.getAlias();
                FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerId, context.getId(),
                        context.getUsername(), context.getToken());
                UserModel user = session.users().getUserByFederatedIdentity(realm, federatedIdentityModel);

                Anvandare userInfo = null;

                if (user != null) {
                    logger.info("preprocessFederatedIdentity: anvandare externtId " + user.getId());
                    // Fetch if anvandare is fully created from our app
                    userInfo = this.databaseAccess.fetchAnvandare(conn, user.getId());
                } else {
                    logger.info("preprocessFederatedIdentity: brokerId " + brokerUserId);
                    // Fetch with brokerId. If we already created anvandare but not yet fully
                    // created it from our app (then uses brokerId as temporary externalId)
                    userInfo = this.databaseAccess.fetchAnvandare(conn, brokerUserId);
                    if (userInfo.Id == null) {
                        userInfo = new Anvandare("", "", "", brokerUserId, null, null);
                    }
                }

                logger.info("Fetched anvandare externtId: " + userInfo.externtId);

                boolean changedData = false;
                if (attribute.equalsIgnoreCase(SSN)
                        && !userInfo.ssn.equalsIgnoreCase(attributeValuesInContext.get(0))) {
                    userInfo.ssn = attributeValuesInContext.get(0);
                    changedData = true;
                }

                if (changedData == true) {
                    // Create or update anvandare
                    this.databaseAccess.updateOrCreateAnvandare(conn, userInfo);
                }
            } catch (Exception e) {
                logger.error("preprocess broker user - failed", e, null, e);
            }
        }
    }

    // @Override
    // public void updateBrokeredUser(KeycloakSession session, RealmModel realm,
    // UserModel user,
    // IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

    // String connectionString = mapperModel.getConfig().get("connectionstring");
    // String maxPoolSize = mapperModel.getConfig().get("maxPoolSize");
    // try {
    // // Set correct driver
    // Class.forName("org.postgresql.Driver");
    // } catch (ClassNotFoundException e) {
    // }

    // String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
    // if (StringUtil.isNullOrEmpty(attribute)) {
    // return;
    // }

    // try (Connection conn = DataSource.getConnection(connectionString,
    // Integer.parseInt(maxPoolSize))) {
    // String userId = user.getId();

    // logger.info("updateBrokeredUser: userId" + userId);

    // Anvandare userInfo = this.databaseAccess.fetchUserInfo(conn, userId);
    // logger.info("Fetched anvandare name: " + userInfo.name);

    // String attributeName = getAttributeNameFromMapperModel(mapperModel);
    // List<String> attributeValuesInContext =
    // findAttributeValuesInContext(attributeName, context);

    // List<String> currentAttributeValues = new ArrayList<String>();
    // List<String> updatedAttributeValues = new ArrayList<String>();

    // if (attribute.equalsIgnoreCase(EMAIL)) {
    // currentAttributeValues.add(userInfo.email);
    // } else if (attribute.equalsIgnoreCase(NAME)) {
    // currentAttributeValues.add(userInfo.name);
    // } else if (attribute.equalsIgnoreCase(SSN)) {
    // currentAttributeValues.add(userInfo.ssn);
    // }

    // if (attributeValuesInContext == null) {
    // // attribute no longer sent by brokered idp, remove it
    // updatedAttributeValues.add("");
    // } else if (currentAttributeValues.size() < 1) {
    // // new attribute sent by brokered idp, add it
    // updatedAttributeValues = attributeValuesInContext;
    // } else if (!CollectionUtil.collectionEquals(attributeValuesInContext,
    // currentAttributeValues)) {
    // // attribute sent by brokered idp has different values as before, update it
    // updatedAttributeValues = attributeValuesInContext;
    // }

    // if (!CollectionUtil.collectionEquals(updatedAttributeValues,
    // currentAttributeValues)) {
    // if (attribute.equalsIgnoreCase(EMAIL)) {
    // userInfo.email = updatedAttributeValues.get(0);
    // } else if (attribute.equalsIgnoreCase(NAME)) {
    // userInfo.name = updatedAttributeValues.get(0);
    // } else if (attribute.equalsIgnoreCase(SSN)) {
    // userInfo.ssn = updatedAttributeValues.get(0);
    // }
    // // this.databaseAccess.updateUserInfo(conn, userId, userInfo);
    // logger.info("updateBrokeredUser: update user to AK: " +
    // userInfo.agronodkontoId + " SSN: "
    // + userInfo.ssn + " EMAIL: "
    // + userInfo.email + " NAME: " + userInfo.name);
    // }

    // } catch (Exception e) {
    // logger.error("update broker user - failed", e, null, e);
    // }
    // }

    @Override
    public String getHelpText() {
        return "Import declared saml attribute if it exists in assertion into the specified Agronod Användare column.";
    }

    // SamlMetadataDescriptorUpdater interface
    @Override
    public void updateMetadata(IdentityProviderMapperModel mapperModel, EntityDescriptorType entityDescriptor) {
        String attributeName = mapperModel.getConfig().get(CustomUserAttributeMapper.ATTRIBUTE_NAME);
        String attributeFriendlyName = mapperModel.getConfig().get(CustomUserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME);

        RequestedAttributeType requestedAttribute = new RequestedAttributeType(attributeName);
        requestedAttribute.setIsRequired(null);
        requestedAttribute
                .setNameFormat(mapperModel.getConfig().get(CustomUserAttributeMapper.ATTRIBUTE_NAME_FORMAT) != null
                        ? JBossSAMLURIConstants
                                .valueOf(mapperModel.getConfig().get(CustomUserAttributeMapper.ATTRIBUTE_NAME_FORMAT))
                                .get()
                        : JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get());

        if (attributeFriendlyName != null && attributeFriendlyName.length() > 0)
            requestedAttribute.setFriendlyName(attributeFriendlyName);

        // Add the requestedAttribute item to any AttributeConsumingServices
        for (EntityDescriptorType.EDTChoiceType choiceType : entityDescriptor.getChoiceType()) {
            List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();
            for (EntityDescriptorType.EDTDescriptorChoiceType descriptor : descriptors) {
                for (AttributeConsumingServiceType attributeConsumingService : descriptor.getSpDescriptor()
                        .getAttributeConsumingService()) {
                    boolean alreadyPresent = attributeConsumingService.getRequestedAttribute().stream()
                            .anyMatch(t -> (attributeName == null || attributeName.equalsIgnoreCase(t.getName())) &&
                                    (attributeFriendlyName == null
                                            || attributeFriendlyName.equalsIgnoreCase(t.getFriendlyName())));

                    if (!alreadyPresent)
                        attributeConsumingService.addRequestedAttribute(requestedAttribute);
                }
            }

        }
    }

    private String getAttributeNameFromMapperModel(IdentityProviderMapperModel mapperModel) {
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (attributeName == null) {
            attributeName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        }
        return attributeName;
    }

    private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
        return attributeType -> {
            AttributeType attribute = attributeType.getAttribute();
            return Objects.equals(attribute.getName(), attributeName)
                    || Objects.equals(attribute.getFriendlyName(), attributeName);
        };
    }

    private List<String> findAttributeValuesInContext(String attributeName, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()
                .flatMap(statement -> statement.getAttributes().stream())
                .filter(elementWith(attributeName))
                .flatMap(attributeType -> attributeType.getAttribute().getAttributeValue().stream())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

}