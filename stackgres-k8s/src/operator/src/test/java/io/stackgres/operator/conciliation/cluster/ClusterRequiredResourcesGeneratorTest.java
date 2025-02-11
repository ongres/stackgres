/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterRequiredResourcesGeneratorTest extends AbstractClusterRequiredResourcesGeneratorTest {

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);
  }

}
