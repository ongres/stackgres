/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDistributedLogsStatusCluster implements KubernetesResource {

  private static final long serialVersionUID = -1L;

  @JsonProperty("namespace")
  private String namespace;

  @JsonProperty("name")
  private String name;

  @JsonProperty("config")
  private StackGresClusterDistributedLogs config;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StackGresClusterDistributedLogs getConfig() {
    return config;
  }

  public void setConfig(StackGresClusterDistributedLogs config) {
    this.config = config;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, namespace, config);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsStatusCluster)) {
      return false;
    }
    StackGresDistributedLogsStatusCluster other = (StackGresDistributedLogsStatusCluster) obj;
    return Objects.equals(namespace, other.namespace)
        && Objects.equals(name, other.name)
        && Objects.equals(config, other.config);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
