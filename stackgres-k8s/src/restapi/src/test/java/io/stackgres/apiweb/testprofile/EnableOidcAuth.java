/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.testprofile;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class EnableOidcAuth implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("stackgres.auth.type", "oidc",
        "quarkus.keycloak.devservices.enabled", "true",
        "quarkus.oidc.public-key", "",
        "quarkus.oidc.application-type", "web-app");
  }

}
