/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.asserThatLabelIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatCronJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatJobResourceLabelsAreComplaints;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatResourceNameIsComplaint;
import static io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper.assertThatStatefulSetResourceLabelsAreComplaints;
import static io.stackgres.operator.validation.CrdMatchTestHelper.getMaxLengthResourceNameFrom;
import static io.stackgres.testutil.StringUtils.getRandomClusterName;

import java.util.List;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.fixture.StackGresBackupConfigFixture;
import io.stackgres.operator.fixture.StackGresBackupFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BackupRequiredResourceDecoratorTest {

  @Inject
  BackupRequiredResourceDecorator resourceDecorator;

  private StackGresBackup crd;

  private StackGresCluster cluster;

  private StackGresBackupConfig backupConfig;

  private ImmutableStackGresBackupContext context;

  private List<HasMetadata> decorateResources;

  @BeforeEach
  public void setup() {
    this.crd = new StackGresBackupFixture().build("default");
    this.cluster = new StackGresClusterFixture().build("default");
    this.backupConfig = new StackGresBackupConfigFixture().build("default");
  }

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws Exception {

    String validBackupName = getRandomClusterName(getMaxLengthResourceNameFrom("SGBackup.yaml"));
    crd.getMetadata().setName(validBackupName);

    this.context = ImmutableStackGresBackupContext.builder()
        .source(crd)
        .cluster(cluster)
        .backupConfig(backupConfig)
        .build();
    this.decorateResources = resourceDecorator.decorateResources(context);
    decorateResources.stream().forEach(
        resource -> {
          assertThatResourceNameIsComplaint(resource.getMetadata().getName());

          resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
          assertThatStatefulSetResourceLabelsAreComplaints(resource);
          assertThatCronJobResourceLabelsAreComplaints(resource);
          assertThatJobResourceLabelsAreComplaints(resource);
        });
  }

}
