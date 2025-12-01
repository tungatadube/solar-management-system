import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: 'http://localhost:8180',
  realm: 'solar-management',
  clientId: 'solar-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
