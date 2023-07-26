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
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceDecoratorTest;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
public class BackupRequiredResourceDecoratorTest
    extends AbstractRequiredResourceDecoratorTest<StackGresBackupContext> {

  @Inject
  BackupRequiredResourceDecorator resourceDecorator;

  private StackGresBackup resource;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  private StackGresBackupConfig backupConfig;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.backup().loadDefault().get();
    this.cluster = Fixtures.cluster().loadDefault().withLatestPostgresVersion().get();
    this.profile = Fixtures.instanceProfile().loadSizeXs().get();
    this.backupConfig = Fixtures.backupConfig().loadDefault().get();
  }

  @Override
  protected String usingKind() {
    return StackGresBackup.KIND;
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
        .foundCluster(cluster)
        .foundProfile(profile)
        .backupConfig(backupConfig)
        .build();
  }

}
