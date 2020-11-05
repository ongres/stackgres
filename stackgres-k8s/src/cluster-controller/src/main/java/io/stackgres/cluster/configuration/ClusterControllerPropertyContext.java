/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.configuration;

import javax.inject.Singleton;

import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.StackGresPropertyContext;

@Singleton
public class ClusterControllerPropertyContext
    implements StackGresPropertyContext<ClusterControllerProperty> {

}
