/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(Include.NON_DEFAULT)
@RegisterForReflection
public class TokenResponse {

  @JsonProperty(value = "access_token", required = true)
  @NotBlank
  private String accessToken;

  @JsonProperty(value = "token_type", required = true)
  @NotBlank
  private String tokenType;

  @JsonProperty(value = "expires_in", required = true)
  private long expiresIn;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(long expiresIn) {
    this.expiresIn = expiresIn;
  }

  @Override
  public String toString() {
    return "TokenResponse [accessToken=" + accessToken + ", tokenType=" + tokenType + ", expiresIn="
        + expiresIn + "]";
  }

}
