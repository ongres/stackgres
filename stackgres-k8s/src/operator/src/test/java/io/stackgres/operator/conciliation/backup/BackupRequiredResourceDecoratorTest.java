/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.AbstractRequiredResourceDecoratorTest;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.fixture.StackGresBackupConfigFixture;
import io.stackgres.operator.fixture.StackGresBackupFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
public class BackupRequiredResourceDecoratorTest
    extends AbstractRequiredResourceDecoratorTest<StackGresBackupContext> {

  @Inject
  BackupRequiredResourceDecorator resourceDecorator;

  private StackGresBackup resource;

  private StackGresCluster cluster;

  private StackGresBackupConfig backupConfig;

  @BeforeEach
  public void setup() {
    this.resource = new StackGresBackupFixture().build("default");
    this.cluster = new StackGresClusterFixture().build("default");
    this.backupConfig = new StackGresBackupConfigFixture().build("default");
  }

  @Override
  protected String usingCrdFilename() {
    return "SGBackup.yaml";
  }

  @Override
  protected HasMetadata getResource() {
    return resource;
  }

  @Override
  protected RequiredResourceDecorator<StackGresBackupContext> getResourceDecorator() {
    return this.resourceDecorator;
  }

  @Override
  public void assertThatResourceNameIsComplaint(HasMetadata resource) {
    ResourceUtil.nameIsValidService(resource.getMetadata().getName());
  }

  @Override
  protected StackGresBackupContext getResourceContext() {
    return ImmutableStackGresBackupContext.builder()
        .source(resource)
        .cluster(cluster)
        .backupConfig(backupConfig)
        .build();
  }

}
