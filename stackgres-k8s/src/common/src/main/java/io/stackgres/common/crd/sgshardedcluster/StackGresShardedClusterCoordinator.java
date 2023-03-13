/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresShardedClusterCoordinator extends StackGresClusterSpec {

  @Override
  public boolean isPosgresSectionPresent() {
    return true;
  }

  @Override
  public boolean isReplicationSectionPresent() {
    return true;
  }

  @Override
  public boolean isSupportingInstancesForInstancesInReplicationGroups() {
    return true;
  }

  @Override
  public boolean isSupportingRequiredSynchronousReplicas() {
    return true;
  }

}
