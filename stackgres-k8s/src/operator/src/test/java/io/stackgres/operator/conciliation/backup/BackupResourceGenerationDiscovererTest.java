/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
public class BackupResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresBackupContext> {

  @Inject
  BackupResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresBackup resource;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  private StackGresObjectStorage objectStorage;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.backup().loadDefault().get();
    this.cluster = Fixtures.cluster().loadDefault().withLatestPostgresVersion().get();
    this.profile = Fixtures.instanceProfile().loadSizeS().get();
    this.objectStorage = Fixtures.objectStorage().loadDefault().get();
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
  protected ResourceGenerationDiscoverer<StackGresBackupContext> getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
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
        .objectStorage(objectStorage)
        .build();
  }

}
