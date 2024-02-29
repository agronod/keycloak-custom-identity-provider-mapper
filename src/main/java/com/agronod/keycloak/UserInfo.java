package com.agronod.keycloak;

import java.util.List;

public class UserInfo {

    public String Id;
    public List<String> Roller;

    public String email = null;
    public String name = null;
    public String ssn = null;
    public String agronodkontoId = null;
    public Boolean registered = false;

    public UserInfo(String email, String name, String ssn, Boolean registered, String agronodkontoId) {
        this.email = email;
        this.name = name;
        this.ssn = ssn;
        this.registered = registered;

        if (agronodkontoId == null || agronodkontoId.isBlank()) {
            agronodkontoId = "55eb2859-e807-4ec2-8eb0-6af3ea64f1a2";
        }
        this.agronodkontoId = agronodkontoId;
    }
}