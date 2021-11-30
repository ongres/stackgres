/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDbOpsBenchmark implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("type")
  @NotEmpty(message = "spec.benchmark.type must be provided")
  private String type;

  @JsonProperty("pgbench")
  @Valid
  private StackGresDbOpsPgbench pgbench;

  @JsonProperty("connectionType")
  private String connectionType;

  @ReferencedField("type")
  interface Type extends FieldReference { }

  @ReferencedField("pgbench")
  interface Pgbench extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "type must be pgbench.",
      payload = Type.class)
  public boolean isTypeValid() {
    return type == null || List.of("pgbench").contains(type);
  }

  @JsonIgnore
  @AssertTrue(message = "pgbench section must be provided.",
      payload = Pgbench.class)
  public boolean isPgbenchSectionProvided() {
    return !Objects.equals(type, "pgbench") || pgbench != null;
  }

  @JsonIgnore
  @AssertTrue(message = "type must be pgbench.",
      payload = Type.class)
  public boolean isConnectionTypeValid() {
    return connectionType == null
        || List.of("primary-service", "replicas-service").contains(connectionType);
  }

  @JsonIgnore
  public boolean isTypePgBench() {
    return Objects.equals(type, "pgbench");
  }

  @JsonIgnore
  public boolean isConnectionTypePrimaryService() {
    return connectionType == null
        || Objects.equals(connectionType, "primary-service");
  }

  @JsonIgnore
  public boolean isConnectionTypeReplicasService() {
    return Objects.equals(connectionType, "replicas-service");
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public StackGresDbOpsPgbench getPgbench() {
    return pgbench;
  }

  public void setPgbench(StackGresDbOpsPgbench pgbench) {
    this.pgbench = pgbench;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionType, pgbench, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsBenchmark)) {
      return false;
    }
    StackGresDbOpsBenchmark other = (StackGresDbOpsBenchmark) obj;
    return Objects.equals(connectionType, other.connectionType)
        && Objects.equals(pgbench, other.pgbench) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
