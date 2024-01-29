/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterCoordinatorConfigurations extends StackGresClusterConfigurations {

  private StackGresShardedClusterShardingSphere shardingSphere;

  public StackGresShardedClusterShardingSphere getShardingSphere() {
    return shardingSphere;
  }

  public void setShardingSphere(StackGresShardedClusterShardingSphere shardingSphere) {
    this.shardingSphere = shardingSphere;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(shardingSphere);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresShardedClusterCoordinatorConfigurations)) {
      return false;
    }
    StackGresShardedClusterCoordinatorConfigurations other = (StackGresShardedClusterCoordinatorConfigurations) obj;
    return Objects.equals(shardingSphere, other.shardingSphere);
  }

}
