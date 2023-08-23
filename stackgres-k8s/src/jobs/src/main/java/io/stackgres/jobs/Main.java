/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs;

import javax.inject.Inject;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.stackgres.jobs.app.JobsProperty;
import io.stackgres.jobs.dbops.DbOpsLauncher;

@QuarkusMain
public class Main implements QuarkusApplication {

  @Inject
  DbOpsLauncher dbOpLauncher;

  @Override
  public int run(String... args) throws Exception {
    String dbOpsCrName = JobsProperty.DATABASE_OPERATION_CR_NAME.getString();
    String jobsNamespace = JobsProperty.JOB_NAMESPACE.getString();
    dbOpLauncher.launchDbOp(dbOpsCrName, jobsNamespace);
    return 0;
  }

}
