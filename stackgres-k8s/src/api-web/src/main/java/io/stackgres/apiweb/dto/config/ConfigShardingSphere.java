/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigShardingSphere {

  private ConfigShardingSphereServiceAccount serviceAccount;

  public ConfigShardingSphereServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ConfigShardingSphereServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
