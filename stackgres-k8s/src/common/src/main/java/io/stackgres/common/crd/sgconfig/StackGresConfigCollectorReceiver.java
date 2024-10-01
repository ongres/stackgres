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
public class StackGresConfigCollectorReceiver {

  private Boolean enabled;

  private Integer exporters;

  private List<StackGresConfigCollectorReceiverDeployment> deployments;

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

  public List<StackGresConfigCollectorReceiverDeployment> getDeployments() {
    return deployments;
  }

  public void setDeployments(List<StackGresConfigCollectorReceiverDeployment> deployments) {
    this.deployments = deployments;
  }

  @Override
  public int hashCode() {
    return Objects.hash(deployments, enabled, exporters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCollectorReceiver)) {
      return false;
    }
    StackGresConfigCollectorReceiver other = (StackGresConfigCollectorReceiver) obj;
    return Objects.equals(deployments, other.deployments) && Objects.equals(enabled, other.enabled)
        && Objects.equals(exporters, other.exporters);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
