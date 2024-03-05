
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.keycloak.models.UserModel;

import com.agronod.keycloak.DataSource;
import com.agronod.keycloak.DatabaseAccess;
import com.agronod.keycloak.Anvandare;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDatabase {

    private final DatabaseAccess databaseAccess = new DatabaseAccess();

    @Test
    public void TestConnection() throws NumberFormatException, SQLException {
        String userId = "1752f7b9-c172-497c-9f8d-bdb7ef0de4a9";
        String jsonKonton = "";
        String maxPoolSize = "2";
        String connectionString = "jdbc:postgresql://localhost:5432/datadelning?currentSchema=public&user=newuser&password=password";

        Anvandare userInfo = null; // new Anvandare(null, null, null, brokerUserId, null, null);
        String attribute ="ssn";

        String user = null; // "169a31e2-b411-43b7-87b1-fb70410cd436";
        String brokerUserId = "999";
        // try (Connection conn = DataSource.getConnection(connectionString, Integer.parseInt(maxPoolSize))) {
        //     if (user != null) {
        //         // Fetch if anvandare is fully created from our app
        //         userInfo = this.databaseAccess.fetchAnvandare(conn, user);
        //     } else {
        //         // Fetch with brokerId. If we already created anvandare but not yet fully
        //         // created it from our app (then uses brokerId as temporary externalId)
        //         userInfo = this.databaseAccess.fetchAnvandare(conn, brokerUserId);
        //         if (userInfo.Id == null) {
        //             userInfo = new Anvandare("", "", "", brokerUserId, null, null);
        //         }
        //     }

        //     List<String> attributeValuesInContext = new ArrayList<String>();
        //     attributeValuesInContext.add("200001122381");
        //     boolean changedData = false;
        //     if (attribute.equalsIgnoreCase("SSN") && !userInfo.ssn.equalsIgnoreCase(attributeValuesInContext.get(0))) {
        //         userInfo.ssn = attributeValuesInContext.get(0);
        //         changedData = true;
        //     }

        //     if (changedData == true) {
        //         // Create or update with brokerId
        //         this.databaseAccess.updateOrCreateAnvandare(conn, userInfo);
        //     }
        // }
     
        // assertEquals(userId, jsonKonton);
    }
}
