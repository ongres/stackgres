/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.dto.dbops.DbOpsSpec;
import io.stackgres.apiweb.dto.dbops.DbOpsStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsTransformerTest {

  @Inject
  DbOpsTransformer transformer;

  public static TransformerTuple<DbOpsDto, StackGresDbOps> createDbOps() {

    StackGresDbOps source = new StackGresDbOps();
    DbOpsDto target = new DbOpsDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.getSource());
    target.setMetadata(metadata.getTarget());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            DbOpsSpec.class,
            StackGresDbOpsSpec.class
        );
    source.setSpec(spec.getSource());
    target.setSpec(spec.getTarget());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            DbOpsStatus.class,
            StackGresDbOpsStatus.class
        );
    source.setStatus(status.getSource());
    target.setStatus(status.getTarget());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testDbOpsTransformation() {

    var tuple = createDbOps();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
