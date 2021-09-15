/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.asserThatLabelIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatCronJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatResourceNameIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatStatefulSetResourceLabelsAreComplaints;
import static io.stackgres.operator.validation.CrdMatchTestHelper.getMaxLengthResourceNameFrom;
import static io.stackgres.testutil.StringUtils.getRandomClusterNameWithExactlySize;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import io.stackgres.operator.fixture.StackGresDbOpsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsRequiredResourceDecoratorTest {

  @Inject
  DbOpsRequiredResourceDecorator resourceDecorator;

  private StackGresDbOps crd;

  private StackGresCluster cluster;

  @BeforeEach
  public void setup() {
    this.crd = new StackGresDbOpsFixture().build("vacuum");
    this.cluster = new StackGresClusterFixture().build("default");
  }

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws Exception {

    String validDbOpsJobName =
        getRandomClusterNameWithExactlySize(getMaxLengthResourceNameFrom("SGDbOps.yaml"));
    crd.getMetadata().setName(validDbOpsJobName);

    var context = ImmutableStackGresDbOpsContext.builder()
        .source(crd)
        .cluster(cluster)
        .build();

    var decorateResources = resourceDecorator.decorateResources(context);
    decorateResources.stream().forEach(
        resource -> {
          assertThatResourceNameIsComplaint(resource);

          resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
            asserThatLabelIsComplaint(label);
          });

          assertThatStatefulSetResourceLabelsAreComplaints(resource);
          assertThatCronJobResourceLabelsAreComplaints(resource);
          assertThatJobResourceLabelsAreComplaints(resource);
        });
  }
}
