/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;

public interface ShardedDbOpsJobFactory {

  Job createJob(StackGresShardedDbOpsContext context);

}
