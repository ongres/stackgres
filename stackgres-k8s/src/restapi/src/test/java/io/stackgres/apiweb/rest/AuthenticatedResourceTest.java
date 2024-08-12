/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.restassured.http.Header;
import io.stackgres.apiweb.security.TokenUtils;

public interface AuthenticatedResourceTest {

  String AUTH_TOKEN = TokenUtils.generateTokenString("admin", "stackgres");

  Header AUTHENTICATION_HEADER = new Header("Authorization", "Bearer " + AUTH_TOKEN);

}
