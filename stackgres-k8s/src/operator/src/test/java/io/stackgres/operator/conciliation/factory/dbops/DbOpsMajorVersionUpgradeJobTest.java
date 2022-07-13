/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DbOpsMajorVersionUpgradeJobTest extends DbOpsJobTestCase {

  @Override
  String fixturePath() {
    return "stackgres_dbops/dbops_majorversionupgrade.json";
  }

}
