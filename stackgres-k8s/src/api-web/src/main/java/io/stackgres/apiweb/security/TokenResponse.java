/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTokenResponse.Builder.class)
@JsonInclude(Include.NON_DEFAULT)
@RegisterForReflection
public interface TokenResponse {

  @JsonProperty(value = "access_token", required = true)
  @NotBlank
  String getAccessToken();

  @JsonProperty(value = "token_type", required = true)
  @NotBlank
  String getTokenType();

  @JsonProperty(value = "expires_in", required = true)
  long getExpiresIn();

}
