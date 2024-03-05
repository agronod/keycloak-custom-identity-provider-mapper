package com.agronod.keycloak;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.jboss.logging.Logger;

public class DatabaseAccess {

    private static Logger logger = Logger.getLogger(DatabaseAccess.class);

    public Anvandare fetchAnvandare(Connection conn, String externalId) {
        String email = null;
        String name = null;
        String ssn = null;
        String externtId = null;
        String id = null;
        String agronodkontoId = null;

        try {
            PreparedStatement st2 = conn.prepareStatement(
                    "select namn, personnummer, epost, externt_id, id, agronodkonto_id from anvandare where externt_id = ?");
            st2.setString(1, externalId);
            ResultSet rs2 = st2.executeQuery();

            while (rs2.next()) {
                name = rs2.getString(1);
                ssn = rs2.getString(2) == null ? "" : rs2.getString(2);
                email = rs2.getString(3);
                externtId = rs2.getString(4);
                id = rs2.getString(5);
                agronodkontoId = rs2.getString(6);
            }

            rs2.close();
            st2.close();
        } catch (Exception e) {
            logger.error("Error fetching anvandare for externalId:" + externalId, e);
        }
        return new Anvandare(email, name, ssn, externtId, id, agronodkontoId);
    }

    public void updateOrCreateAnvandare(Connection conn, Anvandare user) {
        try {
            if (user.agronodkontoId == null) {
                // Create AgronodKonto
                PreparedStatement ste = conn.prepareStatement(
                        "insert into agronodkonto (namn) values('') RETURNING id");
                ResultSet rse = ste.executeQuery();

                while (rse.next()) {
                    user.agronodkontoId = rse.getString(1);
                }

                ste.close();
            }
            PreparedStatement stu = conn.prepareStatement(
                    "insert into anvandare (externt_id, agronodkonto_id, personnummer) values(?,?::uuid,?) on conflict (externt_id) do update set personnummer = EXCLUDED.personnummer;");
            stu.setString(1, user.externtId);
            stu.setString(2, user.agronodkontoId);
            stu.setString(3, user.ssn);
            stu.executeUpdate();

            stu.close();
        } catch (Exception e) {
            logger.error("Error updating anvandare for externtId:" + user.externtId, e);
        }
        return;
    }
}
