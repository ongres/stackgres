/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterReplicateFromInstance {

  @JsonProperty("sgCluster")
  @Valid
  private String sgCluster;

  @JsonProperty("external")
  @Valid
  private StackGresClusterReplicateFromExternal external;

  @ReferencedField("sgCluster")
  interface SgCluster extends FieldReference { }

  @ReferencedField("external")
  interface External extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "One of sgCluster or external is required",
      payload = { SgCluster.class, External.class })
  public boolean isSgClusterOrExternalNotNull() {
    return !(sgCluster == null
        && external == null);
  }

  @JsonIgnore
  @AssertTrue(message = "sgCluster and external are mutually exclusive",
      payload = { SgCluster.class, External.class })
  public boolean isSgClusterOrExternalMutuallyExclusive() {
    return (external == null && sgCluster == null)
        || (external == null && sgCluster != null)
        || (external != null && sgCluster == null);
  }

  public String getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(String sgCluster) {
    this.sgCluster = sgCluster;
  }

  public StackGresClusterReplicateFromExternal getExternal() {
    return external;
  }

  public void setExternal(StackGresClusterReplicateFromExternal external) {
    this.external = external;
  }

  @Override
  public int hashCode() {
    return Objects.hash(external, sgCluster);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicateFromInstance)) {
      return false;
    }
    StackGresClusterReplicateFromInstance other = (StackGresClusterReplicateFromInstance) obj;
    return Objects.equals(external, other.external) && Objects.equals(sgCluster, other.sgCluster);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
