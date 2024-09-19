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
public class ConfigCollectorPrometheusOperator {

  private Boolean allowDiscovery;

  private List<ConfigCollectorPrometheusOperatorMonitor> monitors;

  public Boolean getAllowDiscovery() {
    return allowDiscovery;
  }

  public void setAllowDiscovery(Boolean allowDiscovery) {
    this.allowDiscovery = allowDiscovery;
  }

  public List<ConfigCollectorPrometheusOperatorMonitor> getMonitors() {
    return monitors;
  }

  public void setMonitors(List<ConfigCollectorPrometheusOperatorMonitor> monitors) {
    this.monitors = monitors;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
