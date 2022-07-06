/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.google.common.hash.Hashing;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.jwt.Claims;

public class TokenUtils {

  private TokenUtils() {
    // utility class
  }

  /**
   * Utility method to generate a JWT string from a JSON resource file that is signed by the
   * privateKey.pem test resource key, possibly with invalid fields.
   *
   * @return the JWT string
   */
  public static String generateTokenString(String k8sUsername, String preferredUsername) {
    return Jwt.claims()
        .claim(Claims.jti.name(), UUID.randomUUID())
        .subject(k8sUsername)
        .claim("stackgres_k8s_username", k8sUsername)
        .preferredUserName(preferredUsername)
        .jws()
        .sign();
  }

  /**
   * Return SHA256 of a password.
   *
   * @param  password the password
   * @return          SHA256 of password
   */
  public static String sha256(String password) {
    return Hashing.sha256()
        .hashString(password, StandardCharsets.UTF_8)
        .toString();
  }

}
