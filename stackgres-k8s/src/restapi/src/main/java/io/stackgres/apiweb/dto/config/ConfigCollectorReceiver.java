/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigCollectorReceiver {

  private Boolean enabled;

  private Integer exporters;

  private List<ConfigCollectorReceiverDeployment> deployments;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Integer getExporters() {
    return exporters;
  }

  public void setExporters(Integer exporters) {
    this.exporters = exporters;
  }

  public List<ConfigCollectorReceiverDeployment> getDeployments() {
    return deployments;
  }

  public void setDeployments(List<ConfigCollectorReceiverDeployment> deployments) {
    this.deployments = deployments;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
