/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.client.CustomResource.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
    })
@Group(CommonDefinition.GROUP)
@Version(CommonDefinition.VERSION)
@Kind(StackGresDbOps.KIND)
@Singular("sgdbops")
@Plural("sgdbops")
public final class StackGresDbOps
    extends CustomResource<StackGresDbOpsSpec, StackGresDbOpsStatus>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

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
  public ObjectMeta getMetadata() {
    return super.getMetadata();
  }

  @Override
  public void setMetadata(ObjectMeta metadata) {
    super.setMetadata(metadata);
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
    return StackGresUtil.toPrettyYaml(this);
  }
}
