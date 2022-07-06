/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresDbOpsContext {

  public abstract StackGresCluster getCluster();

  public abstract StackGresProfile getProfile();

  public abstract StackGresDbOps getCurrentDbOps();

}
