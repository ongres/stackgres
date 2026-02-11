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
public class ConfigDeploy {

  private Boolean operator;

  private Boolean restapi;

  private Boolean collector;

  public Boolean getOperator() {
    return operator;
  }

  public void setOperator(Boolean operator) {
    this.operator = operator;
  }

  public Boolean getRestapi() {
    return restapi;
  }

  public void setRestapi(Boolean restapi) {
    this.restapi = restapi;
  }

  public Boolean getCollector() {
    return collector;
  }

  public void setCollector(Boolean collector) {
    this.collector = collector;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
