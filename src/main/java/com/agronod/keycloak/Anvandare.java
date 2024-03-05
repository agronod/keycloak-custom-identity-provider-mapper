package com.agronod.keycloak;

public class Anvandare {

    public String Id = null;
    public String email = null;
    public String name = null;
    public String ssn = null;
    public String externtId = null;
    public String agronodkontoId = null;

    public Anvandare(String email, String name, String ssn, String externtId, String id, String agronodkontoId) {
        this.email = email;
        this.name = name;
        this.ssn = ssn;
        this.externtId = externtId;
        this.Id = id;
        this.agronodkontoId = agronodkontoId;
    }
}