/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.apiweb.dto.config.ConfigSpec;
import io.stackgres.apiweb.dto.config.ConfigStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import org.junit.jupiter.api.Test;

public class ConfigTransformerTest {

  private final JsonMapper mapper = JsonMapper.builder().build();
  ConfigTransformer transformer = new ConfigTransformer(
      mapper
  );

  public static TransformerTuple<ConfigDto, StackGresConfig> createConfig() {
    var metadataTuple = TransformerTestUtil.createMetadataTuple();
    StackGresConfig crd = new StackGresConfig();
    ConfigDto dto = new ConfigDto();

    crd.setMetadata(metadataTuple.source());
    dto.setMetadata(metadataTuple.target());

    var specTuple = TransformerTestUtil
        .fillTupleWithRandomData(ConfigSpec.class, StackGresConfigSpec.class);

    crd.setSpec(specTuple.source());
    dto.setSpec(specTuple.target());

    var statusTuple = TransformerTestUtil
        .fillTupleWithRandomData(ConfigStatus.class, StackGresConfigStatus.class);

    crd.setStatus(statusTuple.source());
    dto.setStatus(statusTuple.target());

    return new TransformerTuple<>(dto, crd);
  }

  @Test
  void testConfigTransformation() {

    var configTuple = createConfig();

    TransformerTestUtil.assertTransformation(
        transformer,
        configTuple);

  }

}
