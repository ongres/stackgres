/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(Include.NON_DEFAULT)
@RegisterForReflection
public record UserPassword(
    @JsonProperty(value = "username", required = true) @NotBlank String username,
    @JsonProperty(value = "password", required = true) @NotBlank String password) {}
