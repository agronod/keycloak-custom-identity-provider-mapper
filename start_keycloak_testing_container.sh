#!/bin/bash

docker rm keycloak-testing-container || true

# --net=host \ is so that we can call localhost from SPI and reach localhost on computer.

docker run \
   --net=host \
   -p 8080:8080 \
   --name keycloak-testing-container \
   -e KEYCLOAK_ADMIN=admin \
   -e KEYCLOAK_ADMIN_PASSWORD=admin \
   -e JAVA_OPTS=-Dkeycloak.profile=preview \
   -v /home/david/Git/Agronod/keycloak-custom-identity-provider-mapper/target/keycloak-custom-identity-providermapper-jar-with-dependencies.jar:/opt/keycloak/providers/keycloak-custom-identity-providermapper-jar-with-dependencies.jar:rw \
   -it quay.io/keycloak/keycloak:18.0.0 \
   start-dev
