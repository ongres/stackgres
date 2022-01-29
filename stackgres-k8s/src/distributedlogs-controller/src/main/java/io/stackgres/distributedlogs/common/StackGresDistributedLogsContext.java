/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.common;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.extension.ExtensionReconciliatorContext;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresDistributedLogsContext
    implements ResourceHandlerContext, ExtensionReconciliatorContext {

  public abstract StackGresDistributedLogs getDistributedLogs();

}
