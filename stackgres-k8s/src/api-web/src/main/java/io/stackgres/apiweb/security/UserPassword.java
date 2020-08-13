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
public class UserPassword {

  @JsonProperty(value = "username", required = true)
  @NotBlank
  private String userName;

  @JsonProperty(value = "password", required = true)
  @NotBlank
  private String password;

  public String getUserName() {
    return userName;
  }

  public void setUserName(String username) {
    this.userName = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "UserPassword [username=" + userName + ", password=" + password + "]";
  }

}
