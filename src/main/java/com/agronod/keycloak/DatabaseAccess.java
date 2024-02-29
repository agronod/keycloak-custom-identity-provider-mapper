package com.agronod.keycloak;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.jboss.logging.Logger;

public class DatabaseAccess {

    private static Logger logger = Logger.getLogger(DatabaseAccess.class);

    public UserInfo fetchUserInfo(Connection conn, String userId) {
        String email = null;
        String name = null;
        String ssn = null;
        String agronodkontoId = null;
        boolean registrerad = false;

        try {
            PreparedStatement st2 = conn.prepareStatement(
                    "select namn, personnummer, epost, registrerad, agronodkonto_id from anvandare where externt_id = ?");
            st2.setString(1, userId);
            ResultSet rs2 = st2.executeQuery();

            while (rs2.next()) {
                name = rs2.getString(1);
                ssn = rs2.getString(2);
                email = rs2.getString(3);
                registrerad = rs2.getBoolean(4);
                agronodkontoId = rs2.getString(5);
            }

            rs2.close();
            st2.close();
        } catch (Exception e) {
            logger.error("Error fetching own user info for userId:" + userId, e);
        }
        return new UserInfo(email, name, ssn, registrerad, agronodkontoId);
    }

    public void updateUserInfo(Connection conn, String userId, UserInfo user) {
        try {

            PreparedStatement stu = conn.prepareStatement(
                    "insert into anvandare (externt_id, namn, personnummer, epost, agronodkonto_id) values (?,?,?,?,?) on conflict (externt_id) do update set namn = EXCLUDED.namn,personnummer = EXCLUDED.personnummer,epost = EXCLUDED.epost;");
            stu.setString(1, userId);
            stu.setString(2, user.name);
            stu.setString(3, user.ssn);
            stu.setString(4, user.email);
            stu.setString(5, user.agronodkontoId);
            int updatedRecords = stu.executeUpdate();

            stu.close();
        } catch (Exception e) {
            logger.error("Error updating user info for externtId:" + userId, e);
        }
        return;
    }
}
