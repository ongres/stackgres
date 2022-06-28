/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresDbOpsContext extends GenerationContext<StackGresDbOps>, ClusterContext {

  StackGresProfile getProfile();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

}
