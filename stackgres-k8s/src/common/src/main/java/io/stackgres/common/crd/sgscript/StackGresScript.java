/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

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
@Kind(StackGresScript.KIND)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public final class StackGresScript
    extends CustomResource<StackGresScriptSpec, StackGresScriptStatus>
    implements Namespaced {

  private static final long serialVersionUID = -1L;

  public static final String KIND = "SGScript";

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresScriptSpec spec;

  @JsonProperty("status")
  @NotNull(message = "The status is required")
  @Valid
  private StackGresScriptStatus status;

  public StackGresScript() {
    super();
  }

  @Override
  public StackGresScriptSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresScriptSpec spec) {
    this.spec = spec;
  }

  @Override
  public StackGresScriptStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus(StackGresScriptStatus status) {
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
    if (!(obj instanceof StackGresScript)) {
      return false;
    }
    StackGresScript other = (StackGresScript) obj;
    return Objects.equals(spec, other.spec)
        && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
