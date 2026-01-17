/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromCustomRestoreMethod;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterReplicateFromExternal {

  @NotEmpty(message = "hosts is required")
  private List<String> hosts;

  @NotEmpty(message = "ports is required")
  private List<Integer> ports;

  private List<StackGresClusterReplicateFromCustomRestoreMethod> customRestoreMethods;

  public List<String> getHosts() {
    return hosts;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public List<Integer> getPorts() {
    return ports;
  }

  public void setPorts(List<Integer> ports) {
    this.ports = ports;
  }

  public List<StackGresClusterReplicateFromCustomRestoreMethod> getCustomRestoreMethods() {
    return customRestoreMethods;
  }

  public void setCustomRestoreMethods(
      List<StackGresClusterReplicateFromCustomRestoreMethod> customRestoreMethods) {
    this.customRestoreMethods = customRestoreMethods;
  }

  @Override
  public int hashCode() {
    return Objects.hash(customRestoreMethods, hosts, ports);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterReplicateFromExternal)) {
      return false;
    }
    StackGresShardedClusterReplicateFromExternal other = (StackGresShardedClusterReplicateFromExternal) obj;
    return Objects.equals(customRestoreMethods, other.customRestoreMethods)
        && Objects.equals(hosts, other.hosts) && Objects.equals(ports, other.ports);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
