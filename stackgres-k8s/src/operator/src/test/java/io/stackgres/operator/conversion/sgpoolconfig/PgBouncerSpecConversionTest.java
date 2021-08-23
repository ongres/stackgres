/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgpoolconfig;

import static io.stackgres.operator.conversion.ConversionUtil.apiVersionAsNumber;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

class PgBouncerSpecConversionTest {

  private final PgBouncerSpecMigration specMigration = new PgBouncerSpecMigration(new JsonMapper());

  private final ObjectNode v1beta1 = JsonUtil
      .readFromJsonAsJson("pooling_config/v1beta1.json");

  private final ObjectNode v1 = JsonUtil
      .readFromJsonAsJson("pooling_config/v1_converted.json");

  @Test
  void givenAPgBouncerBeta1ConfigurationWithDesiredVersion1_shouldMigrateToVersion1() {

    long originalVersion = apiVersionAsNumber("stackgres.io/v1beta1");
    long desiredVersion = apiVersionAsNumber("stackgres.io/v1");

    ObjectNode actualV1 = specMigration.convert(originalVersion, desiredVersion, v1beta1);

    assertEquals(v1.get("spec"), actualV1.get("spec"));
  }

  @Test
  void givenAPgBouncerV1ConfigurationWithDesiredVersionBeta1_shouldMigrateToVersionBeta1() {

    long originalVersion = apiVersionAsNumber("stackgres.io/v1");
    long desiredVersion = apiVersionAsNumber("stackgres.io/v1beta1");

    ObjectNode actualV1Beta1 = specMigration.convert(originalVersion, desiredVersion, v1);

    assertEquals(v1beta1.get("spec"), actualV1Beta1.get("spec"));

  }
}
