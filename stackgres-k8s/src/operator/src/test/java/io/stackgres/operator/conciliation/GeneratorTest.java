/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgcluster.StackGresReplicationRole;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.ClusterResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

public abstract class GeneratorTest {

  protected static final String CLUSTER_NAME = StringUtils.getRandomResourceName(10);
  protected static final String CLUSTER_NAMESPACE = StringUtils.getRandomResourceName(10);
  protected static final String CLUSTER_UID = UUID.randomUUID().toString();
  protected StackGresCluster cluster;
  protected StackGresProfile stackGresProfile;
  protected StackGresPostgresConfig stackGresPostgresConfig;

  @Inject
  ClusterResourceGenerationDiscoverer resourceGenerationDiscoverer;

  public GeneratedResourceMatcher givenAClusterWithVersion(StackGresVersion version) {
    cluster.setMetadata(new ObjectMeta());
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(CLUSTER_NAMESPACE);
    cluster.getMetadata().setUid(CLUSTER_UID);
    cluster.getMetadata().setAnnotations(new HashMap<>());
    cluster.getMetadata().getAnnotations().put("stackgres.io/operatorVersion",
        version.getVersion());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    String defaultPostgreVersion = StackGresComponent.POSTGRESQL
        .getOrThrow(version).getLatestVersion();
    cluster.getSpec().getPostgres().setVersion(defaultPostgreVersion);
    cluster.getSpec().setReplication(new StackGresClusterReplication());
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.ASYNC.toString());
    cluster.getSpec().getReplication().setRole(StackGresReplicationRole.HA_READ.toString());
    cluster.getSpec().setPods(new StackGresClusterPods());
    cluster.getSpec().getPods().setPersistentVolume(new StackGresClusterPodsPersistentVolume());
    cluster.getSpec().getPods().getPersistentVolume().setSize("500Mi");

    return GeneratedResourceMatcher.givenACluster(cluster, resourceGenerationDiscoverer)
        .andInstanceProfile(stackGresProfile)
        .andPostgresConfig(stackGresPostgresConfig);
  }

  protected List<HasMetadata> getResources(StackGresClusterContext context) {
    return resourceGenerationDiscoverer.generateResources(context);
  }

  protected List<HasMetadata> getResources() {
    var context = buildContext();
    return getResources(context);
  }

  private StackGresClusterContext buildContext() {
    return StackGresClusterContext.builder()
        .source(cluster)
        .profile(stackGresProfile)
        .postgresConfig(stackGresPostgresConfig)
        .build();
  }

  @BeforeEach
  void setUp() {
    stackGresProfile = Fixtures.instanceProfile().loadSizeS().get();
    stackGresProfile.getMetadata().setNamespace(CLUSTER_NAMESPACE);

    stackGresPostgresConfig = Fixtures.postgresConfig().loadDefault().get();

    cluster = new StackGresCluster();
    cluster.getMetadata().setUid(CLUSTER_UID);
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(CLUSTER_NAMESPACE);
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    cluster.getSpec().getConfigurations().setSgPostgresConfig(stackGresPostgresConfig.getMetadata()
        .getName());
    cluster.getSpec().setSgInstanceProfile(stackGresProfile.getMetadata().getName());
  }

}
