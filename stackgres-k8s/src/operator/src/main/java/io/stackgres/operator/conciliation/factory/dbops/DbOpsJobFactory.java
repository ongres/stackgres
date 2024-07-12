/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;

public interface DbOpsJobFactory {

  Job createJob(StackGresDbOpsContext context);

}
