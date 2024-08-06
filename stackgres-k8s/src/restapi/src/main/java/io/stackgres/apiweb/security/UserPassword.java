/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(Include.NON_DEFAULT)
@RegisterForReflection
public record UserPassword(
    @NotBlank(message = "username is required and con not be blank")
    String username,
    @NotBlank(message = "password is required and con not be blank")
    String password) {}
