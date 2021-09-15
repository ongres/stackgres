/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.asserThatLabelIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatCronJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatResourceNameIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatStatefulSetResourceLabelsAreComplaints;
import static io.stackgres.operator.validation.CrdMatchTestHelper.getMaxLengthResourceNameFrom;
import static io.stackgres.testutil.StringUtils.getRandomClusterNameWithExactlySize;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.fixture.SecretFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import io.stackgres.operator.fixture.StackGresDistributedLogsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DistributedLogsRequireResourceDecoratorTest {

  @Inject
  private DistributedLogsRequireResourceDecorator resourceDecorator;

  private StackGresDistributedLogs crd;

  private Optional<Secret> secret;

  private StackGresCluster connectecCluster;

  @BeforeEach
  public void setup() {
    this.crd = new StackGresDistributedLogsFixture().build("default");
    this.connectecCluster = new StackGresClusterFixture().build("default");
    this.secret = ofNullable(new SecretFixture().build("minio"));
  }

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength()
      throws JsonProcessingException, IOException {

    String validClusterName =
        getRandomClusterNameWithExactlySize(getMaxLengthResourceNameFrom("SGDistributedLogs.yaml"));
    crd.getMetadata().setName(validClusterName);

    StackGresDistributedLogsContext context = ImmutableStackGresDistributedLogsContext.builder()
        .source(crd)
        .addAllConnectedClusters(List.of(connectecCluster))
        .databaseCredentials(secret)
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
