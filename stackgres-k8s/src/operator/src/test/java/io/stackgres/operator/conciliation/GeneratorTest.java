/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.cluster.ClusterRequiredResourceDecorator;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;

public abstract class GeneratorTest {

  protected static final String CLUSTER_NAME = StringUtils.getRandomClusterName(10);
  protected static final String CLUSTER_NAMESPACE = StringUtils.getRandomClusterName(10);
  protected static final String CLUSTER_UID = UUID.randomUUID().toString();
  protected StackGresCluster cluster;
  protected StackGresProfile stackGresProfile;
  protected StackGresPostgresConfig stackGresPostgresConfig;

  @Inject
  ClusterRequiredResourceDecorator resourceGenerationDiscoverer;

  public GeneratedResourceMatcher givenAClusterWithVersion(StackGresVersion version) {
    cluster.setMetadata(new ObjectMeta());
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(CLUSTER_NAMESPACE);
    cluster.getMetadata().setUid(CLUSTER_UID);
    cluster.getMetadata().setAnnotations(new HashMap<>());
    cluster.getMetadata().getAnnotations().put("stackgres.io/operatorVersion",
        version.getVersion());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    String defaultPostgreVersion;
    if (version.ordinal() <= StackGresVersion.V09_LAST.ordinal()) {
      defaultPostgreVersion = "12.6";
    } else {
      defaultPostgreVersion = StackGresComponent.POSTGRESQL.findLatestVersion();
    }
    cluster.getSpec().getPostgres().setVersion(defaultPostgreVersion);
    cluster.getSpec().setPod(new StackGresClusterPod());
    cluster.getSpec().getPod().setPersistentVolume(new StackGresPodPersistentVolume());
    cluster.getSpec().getPod().getPersistentVolume().setSize("500Mi");

    return GeneratedResourceMatcher.givenACluster(cluster, resourceGenerationDiscoverer)
        .andInstanceProfile(stackGresProfile)
        .andPostgresConfig(stackGresPostgresConfig);
  }

  protected List<HasMetadata> getResources(StackGresClusterContext context) {
    return resourceGenerationDiscoverer.decorateResources(context);
  }

  protected List<HasMetadata> getResources() {
    var context = buildContext();
    return getResources(context);
  }

  private StackGresClusterContext buildContext() {
    return ImmutableStackGresClusterContext.builder()
        .source(cluster)
        .stackGresProfile(stackGresProfile)
        .postgresConfig(stackGresPostgresConfig)
        .build();
  }

  @BeforeEach
  void setUp() {
    stackGresProfile = JsonUtil.readFromJson("stackgres_profiles/size-xs.json",
        StackGresProfile.class);
    stackGresProfile.getMetadata().setNamespace(CLUSTER_NAMESPACE);

    stackGresPostgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);

    cluster = new StackGresCluster();
    cluster.getMetadata().setUid(CLUSTER_UID);
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(CLUSTER_NAMESPACE);
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setConfiguration(new StackGresClusterConfiguration());
    cluster.getSpec().getConfiguration().setPostgresConfig(stackGresPostgresConfig.getMetadata()
        .getName());
    cluster.getSpec().setResourceProfile(stackGresProfile.getMetadata().getName());
  }

}
