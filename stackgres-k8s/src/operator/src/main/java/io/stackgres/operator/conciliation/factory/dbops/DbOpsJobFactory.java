/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.StackGresProperty;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;

public interface DbOpsJobFactory {

  String IMAGE_TEMPLATE = "%s/stackgres/jobs:%s";

  Job createJob(StackGresDbOpsContext context);

  default String getImageName() {
    return String.format(IMAGE_TEMPLATE,
        getContainerRegistry(),
        StackGresProperty.OPERATOR_IMAGE_VERSION.getString());
  }

  default String getContainerRegistry() {
    return StackGresProperty.SG_CONTAINER_REGISTRY.getString();
  }

}
