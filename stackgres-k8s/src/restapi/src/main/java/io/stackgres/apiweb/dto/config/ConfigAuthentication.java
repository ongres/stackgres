/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigAuthentication {

  private String type;

  private Boolean createAdminSecret;

  private String user;

  private String password;

  private SecretKeySelector secretRef;

  private ConfigAuthenticationOidc oidc;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Boolean getCreateAdminSecret() {
    return createAdminSecret;
  }

  public void setCreateAdminSecret(Boolean createAdminSecret) {
    this.createAdminSecret = createAdminSecret;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public SecretKeySelector getSecretRef() {
    return secretRef;
  }

  public void setSecretRef(SecretKeySelector secretRef) {
    this.secretRef = secretRef;
  }

  public ConfigAuthenticationOidc getOidc() {
    return oidc;
  }

  public void setOidc(ConfigAuthenticationOidc oidc) {
    this.oidc = oidc;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
