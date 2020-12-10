/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class StackGresDbOps extends CustomResource {

  private static final long serialVersionUID = 1L;

  @NotNull(message = "The specification is required")
  @Valid
  private StackGresDbOpsSpec spec;

  @Valid
  private StackGresDbOpsStatus status;

  public StackGresDbOps() {
    super(StackGresDbOpsDefinition.KIND);
  }

  public StackGresDbOpsSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresDbOpsSpec spec) {
    this.spec = spec;
  }

  public StackGresDbOpsStatus getStatus() {
    return status;
  }

  public void setStatus(StackGresDbOpsStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOps)) {
      return false;
    }
    StackGresDbOps other = (StackGresDbOps) obj;
    return Objects.equals(spec, other.spec) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
