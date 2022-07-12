/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Group(CommonDefinition.GROUP)
@Version(CommonDefinition.VERSION)
@Kind(StackGresDbOps.KIND)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public final class StackGresDbOps
    extends CustomResource<StackGresDbOpsSpec, StackGresDbOpsStatus>
    implements Namespaced {

  private static final long serialVersionUID = 4405727567343191955L;

  public static final String KIND = "SGDbOps";

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresDbOpsSpec spec;

  @JsonProperty("status")
  @Valid
  private StackGresDbOpsStatus status;

  public StackGresDbOps() {
    super();
  }

  @Override
  public StackGresDbOpsSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresDbOpsSpec spec) {
    this.spec = spec;
  }

  @Override
  public StackGresDbOpsStatus getStatus() {
    return status;
  }

  @Override
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
    return Objects.equals(spec, other.spec)
        && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    // TODO: review if this should return a YAML or keep the same format of toString().
    // if the yaml is needed it should be called from another method.
    return StackGresUtil.toPrettyYaml(this);
  }
}
