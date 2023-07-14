/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
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
@Version(StackGresShardedCluster.VERSION)
@Kind(StackGresShardedCluster.KIND)
public final class StackGresShardedCluster
    extends CustomResource<StackGresShardedClusterSpec, StackGresShardedClusterStatus>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

  public static final String KIND = "SGShardedCluster";

  public static final String VERSION = "v1alpha1";

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresShardedClusterSpec spec;

  @JsonProperty("status")
  @Valid
  private StackGresShardedClusterStatus status;

  public StackGresShardedCluster() {
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
  public StackGresShardedClusterSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresShardedClusterSpec spec) {
    this.spec = spec;
  }

  @Override
  public StackGresShardedClusterStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus(StackGresShardedClusterStatus status) {
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
    if (!(obj instanceof StackGresShardedCluster)) {
      return false;
    }
    StackGresShardedCluster other = (StackGresShardedCluster) obj;
    return Objects.equals(spec, other.spec)
        && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
