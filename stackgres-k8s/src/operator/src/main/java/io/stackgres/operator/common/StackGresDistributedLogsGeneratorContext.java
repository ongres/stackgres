/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import org.immutables.value.Value.Immutable;

@Immutable
public interface StackGresDistributedLogsGeneratorContext extends StackGresGeneratorContext {

  StackGresDistributedLogsContext getDistributedLogsContext();

  @Override
  default StackGresClusterContext getClusterContext() {
    return getDistributedLogsContext();
  }

}
