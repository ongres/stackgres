/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;

@ApplicationScoped
public class ShardedDbOpsEventEmitter extends AbstractEventEmitter<StackGresShardedDbOps> {

}
