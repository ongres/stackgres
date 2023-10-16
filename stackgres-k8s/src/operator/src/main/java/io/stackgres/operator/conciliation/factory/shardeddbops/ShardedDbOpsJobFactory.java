/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.StackGresProperty;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;

public interface ShardedDbOpsJobFactory {

  String IMAGE_TEMPLATE = "%s/stackgres/jobs:%s";

  Job createJob(StackGresShardedDbOpsContext context);

  default String getImageName() {
    return String.format(IMAGE_TEMPLATE,
        getContainerRegistry(),
        StackGresProperty.OPERATOR_IMAGE_VERSION.getString());
  }

  default String getContainerRegistry() {
    return StackGresProperty.SG_CONTAINER_REGISTRY.getString();
  }

  default String getPullPolicy() {
    return StackGresProperty.SG_IMAGE_PULL_POLICY.getString();
  }

}
