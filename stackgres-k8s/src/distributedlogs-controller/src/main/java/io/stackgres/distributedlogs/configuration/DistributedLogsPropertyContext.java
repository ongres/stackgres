/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.configuration;

import javax.inject.Singleton;

import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.distributedlogs.common.DistributedLogsProperty;

@Singleton
public class DistributedLogsPropertyContext
    implements StackGresPropertyContext<DistributedLogsProperty> {

}
