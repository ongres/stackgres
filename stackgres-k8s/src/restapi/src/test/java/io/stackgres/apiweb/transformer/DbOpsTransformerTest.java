/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.dto.dbops.DbOpsSpec;
import io.stackgres.apiweb.dto.dbops.DbOpsStatus;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class DbOpsTransformerTest {

  @Inject
  DbOpsTransformer transformer;

  public static TransformerTuple<DbOpsDto, StackGresDbOps> createDbOps() {

    StackGresDbOps source = new StackGresDbOps();
    DbOpsDto target = new DbOpsDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            DbOpsSpec.class,
            StackGresDbOpsSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            DbOpsStatus.class,
            StackGresDbOpsStatus.class
        );
    source.setStatus(status.source());
    target.setStatus(status.target());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testDbOpsTransformation() {

    var tuple = createDbOps();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
