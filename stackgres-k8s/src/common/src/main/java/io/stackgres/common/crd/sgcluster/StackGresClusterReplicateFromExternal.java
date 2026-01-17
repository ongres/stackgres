/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterReplicateFromExternal {

  @NotNull(message = "host is required")
  private String host;

  @NotNull(message = "port is required")
  private Integer port;

  private StackGresClusterReplicateFromCustomRestoreMethod customRestoreMethod;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public StackGresClusterReplicateFromCustomRestoreMethod getCustomRestoreMethod() {
    return customRestoreMethod;
  }

  public void setCustomRestoreMethod(
      StackGresClusterReplicateFromCustomRestoreMethod customRestoreMethod) {
    this.customRestoreMethod = customRestoreMethod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(customRestoreMethod, host, port);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicateFromExternal)) {
      return false;
    }
    StackGresClusterReplicateFromExternal other = (StackGresClusterReplicateFromExternal) obj;
    return Objects.equals(customRestoreMethod, other.customRestoreMethod)
        && Objects.equals(host, other.host) && Objects.equals(port, other.port);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
