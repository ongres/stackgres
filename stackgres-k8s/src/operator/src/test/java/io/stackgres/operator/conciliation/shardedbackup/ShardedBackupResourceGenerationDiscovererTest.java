/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.getShardClusterName;

import java.util.stream.Stream;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
public class ShardedBackupResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresShardedBackupContext> {

  @Inject
  ShardedBackupResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresShardedBackup backup;

  private StackGresShardedCluster cluster;

  private StackGresProfile profile;

  @BeforeEach
  public void setup() {
    this.backup = Fixtures.shardedBackup().loadDefault().get();
    this.cluster = Fixtures.shardedCluster().loadDefault().withLatestPostgresVersion().get();
    this.profile = Fixtures.instanceProfile().loadSizeXs().get();
  }

  @Override
  protected Stream<? extends HasMetadata> jobGeneratedResources() {
    return Seq.of(getCoordinatorClusterName(cluster))
        .append(Seq.range(0, cluster.getSpec().getShards().getClusters())
            .map(index -> getShardClusterName(cluster, index)))
        .map(n -> backup.getMetadata().getName()
            + n.substring(cluster.getMetadata().getName().length()))
        .map(backupName -> new StackGresBackupBuilder()
            .withNewMetadata()
            .withName(backupName)
            .endMetadata()
            .build());
  }

  @Override
  protected String usingKind() {
    return StackGresBackup.KIND;
  }

  @Override
  protected HasMetadata getResource() {
    return backup;
  }

  @Override
  protected ResourceGenerationDiscoverer<StackGresShardedBackupContext>
      getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
  }

  @Override
  public void assertThatResourceNameIsComplaint(HasMetadata resource) {
    ResourceUtil.nameIsValidService(resource.getMetadata().getName());
  }

  @Override
  protected StackGresShardedBackupContext getResourceContext() {
    return ImmutableStackGresShardedBackupContext.builder()
        .source(backup)
        .foundShardedCluster(cluster)
        .foundProfile(profile)
        .build();
  }

}
