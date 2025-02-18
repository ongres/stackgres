/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DistributedLogsRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  PostgresConfigFinder postgresConfigFinder;

  @InjectMock
  ProfileFinder profileFinder;

  @Inject
  DistributedLogsRequiredResourcesGenerator generator;

  private StackGresConfig config;
  private StackGresDistributedLogs distributedLogs;
  private StackGresPostgresConfig postgresConfig;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeS().get();
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
  }

  @Test
  void givenValidDistributedLogs_shouldPass() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(postgresConfig));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    generator.getRequiredResources(distributedLogs);
  }

}
