/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigAuthentication {

  private String type;

  private ConfigAuthenticationOidc oidc;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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
