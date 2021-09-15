/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.asserThatLabelIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatCronJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatResourceNameIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatStatefulSetResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.getTestInitScripts;
import static io.stackgres.operator.validation.CrdMatchTestHelper.getMaxLengthResourceNameFrom;
import static io.stackgres.testutil.StringUtils.getRandomClusterNameWithExactlySize;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.fixture.SecretFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import io.stackgres.operator.fixture.StackGresPoolingConfigFixture;
import io.stackgres.operator.fixture.StackGresPostgresConfigFixture;
import io.stackgres.operator.fixture.StackGresProfileFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
class ClusterRequiredResourceDecoratorTest {

  @Inject
  ClusterRequiredResourceDecorator resourceDecorator;

  private StackGresCluster crd;
  private StackGresPostgresConfig clusterPgConfig;
  private StackGresProfile clusterProfile;
  private Optional<StackGresBackupConfig> backupConfig;
  private Optional<StackGresPoolingConfig> clusterPooling;
  private Optional<Secret> secret;
  private static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions().get(0).get();

  @BeforeEach
  public void setup() {
    this.crd = new StackGresClusterFixture().build("default");
    setupPostgresLatestVersion();
    this.clusterPgConfig = new StackGresPostgresConfigFixture().build("default_postgres");
    this.clusterProfile = new StackGresProfileFixture().build("size-s");
    this.backupConfig = ofNullable(null);
    this.clusterPooling = ofNullable(new StackGresPoolingConfigFixture().build("default"));
    this.secret = ofNullable(new SecretFixture().build("minio"));
  }

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws IOException {

    String validClusterName =
        getRandomClusterNameWithExactlySize(getMaxLengthResourceNameFrom("SGCluster.yaml"));
    crd.getMetadata().setName(validClusterName);

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(crd)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .backupConfig(backupConfig)
        .poolingConfig(clusterPooling)
        .prometheus(new Prometheus(false, null))
        .internalScripts(List.of(getTestInitScripts()))
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

  private void setupPostgresLatestVersion() {
    this.crd.getSpec().getPostgres().setVersion(POSTGRES_LATEST_VERSION);

  }

}
