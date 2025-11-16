/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterReplicateFromInstance {

  @Valid
  private String sgShardedCluster;

  @Valid
  private StackGresShardedClusterReplicateFromExternal external;

  @ReferencedField("sgShardedCluster")
  interface SgShardedCluster extends FieldReference { }

  @ReferencedField("external")
  interface External extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "One of sgShardedCluster or external is required",
      payload = { SgShardedCluster.class, External.class })
  public boolean isSgShardedClusterOrExternalNotNull() {
    return !(sgShardedCluster == null
        && external == null);
  }

  @JsonIgnore
  @AssertTrue(message = "sgShardedCluster and external are mutually exclusive",
      payload = { SgShardedCluster.class, External.class })
  public boolean isSgShardedClusterOrExternalMutuallyExclusive() {
    return (external == null && sgShardedCluster == null)
        || (external == null && sgShardedCluster != null)
        || (external != null && sgShardedCluster == null);
  }

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public StackGresShardedClusterReplicateFromExternal getExternal() {
    return external;
  }

  public void setExternal(StackGresShardedClusterReplicateFromExternal external) {
    this.external = external;
  }

  @Override
  public int hashCode() {
    return Objects.hash(external, sgShardedCluster);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterReplicateFromInstance)) {
      return false;
    }
    StackGresShardedClusterReplicateFromInstance other = (StackGresShardedClusterReplicateFromInstance) obj;
    return Objects.equals(external, other.external) && Objects.equals(sgShardedCluster, other.sgShardedCluster);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
