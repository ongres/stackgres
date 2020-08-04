/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.restassured.http.Header;
import io.stackgres.apiweb.security.TokenUtils;

public interface AuthenticatedResourceTest {

  Header AUTHENTICATION_HEADER = new Header("Authorization", "Bearer "
      + TokenUtils.generateTokenString(
      "admin",
      "stackgres",
      10000,
      "src/test/resources/jwt/rsa_private.key"));
}
