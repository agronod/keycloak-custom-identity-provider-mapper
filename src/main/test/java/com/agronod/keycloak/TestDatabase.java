
import java.sql.Connection;
import java.util.List;

import org.junit.Test;

import com.agronod.keycloak.DataSource;
import com.agronod.keycloak.DatabaseAccess;
import com.agronod.keycloak.UserInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDatabase {

    private final DatabaseAccess databaseAccess = new DatabaseAccess();

    @Test
    public void TestConnection() {
        String userId = "1752f7b9-c172-497c-9f8d-bdb7ef0de4a9";
        String jsonKonton = "";
        String maxPoolSize = "2";
        String connectionString = "jdbc:postgresql://localhost:5432/datadelning?currentSchema=public&user=newuser&password=password";

        // try (Connection conn = DataSource.getConnection(connectionString, Integer.parseInt(maxPoolSize))) {
        //     System.setProperty("DB_JDBC_URL", connectionString);
        //     List<AgronodKonton> konton = this.databaseAccess.fetchOwnAgroKontoWithAffarspartners(conn,
        //             userId);

        //     UserInfo userInfo = this.databaseAccess.fetchUserInfo(conn, userId);

        //     // Admin roles
        //     konton = this.databaseAccess.fetchAdminRoles(conn, userId, konton);

        //     ObjectMapper mapper = new ObjectMapper();
        //     mapper.setSerializationInclusion(Include.NON_NULL);
        //     jsonKonton = mapper.writeValueAsString(konton);

        // } catch (Exception e) {
        // }

        // assertEquals(userId, jsonKonton);
    }
}
