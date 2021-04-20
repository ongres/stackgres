/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.configuration;

import javax.inject.Singleton;

import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.StackGresPropertyContext;

@Singleton
public class DistributedLogsControllerPropertyContext
    implements StackGresPropertyContext<DistributedLogsControllerProperty> {

}
