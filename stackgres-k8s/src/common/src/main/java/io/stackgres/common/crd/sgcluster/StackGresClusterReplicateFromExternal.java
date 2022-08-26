/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFromExternal {

  @JsonProperty("host")
  @NotNull(message = "host is required")
  private String host;

  @JsonProperty("port")
  @NotNull(message = "port is required")
  private Integer port;

  @JsonProperty("secretKeyRefs")
  @NotNull(message = "secretKeyRefs section is required")
  @Valid
  private StackGresClusterReplicateFromExternalSecretKeyRefs secretKeyRefs;

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

  public StackGresClusterReplicateFromExternalSecretKeyRefs getSecretKeyRefs() {
    return secretKeyRefs;
  }

  public void setSecretKeyRefs(StackGresClusterReplicateFromExternalSecretKeyRefs secretKeyRefs) {
    this.secretKeyRefs = secretKeyRefs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, secretKeyRefs);
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
    return Objects.equals(host, other.host) && Objects.equals(port, other.port)
        && Objects.equals(secretKeyRefs, other.secretKeyRefs);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
