/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.StackGresDefaultKubernetesClient;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;

//@QuarkusTest
@WithKubernetesTestServer
class ClusterRequiredResourceDecoratorTest extends RequiredResourceDecoratorTestHelper {

  @Inject
  ClusterRequiredResourceDecorator resourceDecorator;

  private StackGresCluster crd;
  private StackGresPostgresConfig clusterPgConfig;
  private StackGresProfile clusterProfile;
  private StackGresBackupConfig backupConfig;
  private StackGresPoolingConfig clusterPooling;
  private Secret secret;

  // @BeforeEach
  public void setup() {
    this.crd = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);
    this.clusterPgConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
    this.clusterProfile = JsonUtil.readFromJson("stackgres_profiles/size-s.json",
        StackGresProfile.class);
    this.backupConfig = JsonUtil.readFromJson("backup_config/default.json",
        StackGresBackupConfig.class);
    this.clusterPooling = JsonUtil.readFromJson("pooling_config/default.json",
        StackGresPoolingConfig.class);
    this.secret = JsonUtil.readFromJson("secret/minio.json",
        Secret.class);

    initializeSecret();
  }

  private void initializeSecret() {
    StackGresDefaultKubernetesClient stackGresKubernetesClient =
        new StackGresDefaultKubernetesClient();
    secret.getMetadata().setNamespace("test");
    stackGresKubernetesClient.secrets().create(secret);
    stackGresKubernetesClient.close();
  }

  // @Test
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
        .internalScripts(List.of(new StackGresClusterScriptEntry()))
        .databaseCredentials(secret)
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
