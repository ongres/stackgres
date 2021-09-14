/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
class ClusterRequiredResourceDecoratorTest extends RequiredResourceDecoratorTestHelper {

  @InjectMock
  SecretFinder finder;
  
  @Inject
  ClusterRequiredResourceDecorator resourceDecorator;

  private StackGresCluster crd;
  private StackGresPostgresConfig clusterPgConfig;
  private StackGresProfile clusterProfile;
  private Optional<StackGresBackupConfig> backupConfig;
  private Optional<StackGresPoolingConfig> clusterPooling;
  private Optional<Secret> secret;

   @BeforeEach
  public void setup() {
    this.crd = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);
    this.clusterPgConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
    this.clusterProfile = JsonUtil.readFromJson("stackgres_profiles/size-s.json",
        StackGresProfile.class);
    this.backupConfig = ofNullable(null);
    this.clusterPooling = ofNullable(JsonUtil.readFromJson("pooling_config/default.json",
        StackGresPoolingConfig.class));
    this.secret = ofNullable(JsonUtil.readFromJson("secret/minio.json",
        Secret.class));
  }

   @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws IOException {
    withSelectedCrd("SGCluster.yaml");

    String validResourceName = StringUtils.getRandomClusterName(withCurrentCrdMaxLength());
    crd.getMetadata().setName(validResourceName);

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(crd)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .backupConfig(backupConfig)
        .poolingConfig(clusterPooling)
        .prometheus(new Prometheus(false, null))
        .internalScripts(List.of(getTestScripts()))
        .databaseCredentials(secret)
        .build();

    var decorateResources = resourceDecorator.decorateResources(context);
    decorateResources.stream().forEach(
        resource -> {
          resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
          
          assertThatStatefulSetResourceLabelsAreComplaints(resource);
          assertThatCronJobResourceLabelsAreComplaints(resource);
          assertThatJobResourceLabelsAreComplaints(resource);
        });

  }

  private StackGresClusterScriptEntry getTestScripts() {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("test-script");
    script.setDatabase("db");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterRequiredResourcesGenerator.class.getResource(
            "/prometheus-postgres-exporter/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

}
