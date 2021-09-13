/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsRequiredResourceDecoratorTest extends RequiredResourceDecoratorTestHelper {

  @Inject
  DbOpsRequiredResourceDecorator resourceDecorator;

  private StackGresDbOps crd;

  private StackGresCluster cluster;

  @BeforeEach
  public void setup() {
    this.crd = JsonUtil.readFromJson("dbops/vacuum.json",
        StackGresDbOps.class);
    this.cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);
  }

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws Exception {

    withSelectedCrd("SGDbOps.yaml");

    String validResourceName = StringUtils.getRandomClusterName(withCurrentCrdMaxLength());
    crd.getMetadata().setName(validResourceName);
    var context = ImmutableStackGresDbOpsContext.builder()
        .source(crd)
        .cluster(cluster)
        .build();

    var decorateResources = resourceDecorator.decorateResources(context);
    decorateResources.stream().forEach(
        resource -> {
          resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
            asserThatLabelIsComplaince(label);
          });
        });
  }
}
