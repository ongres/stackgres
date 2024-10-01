/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigCollectorPrometheusOperator {

  private Boolean allowDiscovery;

  private List<StackGresConfigCollectorPrometheusOperatorMonitor> monitors;

  public Boolean getAllowDiscovery() {
    return allowDiscovery;
  }

  public void setAllowDiscovery(Boolean allowDiscovery) {
    this.allowDiscovery = allowDiscovery;
  }

  public List<StackGresConfigCollectorPrometheusOperatorMonitor> getMonitors() {
    return monitors;
  }

  public void setMonitors(List<StackGresConfigCollectorPrometheusOperatorMonitor> monitors) {
    this.monitors = monitors;
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowDiscovery, monitors);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCollectorPrometheusOperator)) {
      return false;
    }
    StackGresConfigCollectorPrometheusOperator other = (StackGresConfigCollectorPrometheusOperator) obj;
    return Objects.equals(allowDiscovery, other.allowDiscovery)
        && Objects.equals(monitors, other.monitors);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
