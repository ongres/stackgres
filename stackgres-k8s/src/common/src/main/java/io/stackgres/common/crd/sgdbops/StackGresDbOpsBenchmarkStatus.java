/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsBenchmarkStatus implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("pgbench")
  private StackGresDbOpsPgbenchStatus pgbench;

  public StackGresDbOpsPgbenchStatus getPgbench() {
    return pgbench;
  }

  public void setPgbench(StackGresDbOpsPgbenchStatus pgbench) {
    this.pgbench = pgbench;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pgbench);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsBenchmarkStatus)) {
      return false;
    }
    StackGresDbOpsBenchmarkStatus other = (StackGresDbOpsBenchmarkStatus) obj;
    return Objects.equals(pgbench, other.pgbench);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
