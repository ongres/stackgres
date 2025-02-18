/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsInstanceProfileContextAppenderTest {

  private DistributedLogsInstanceProfileContextAppender contextAppender;

  private DefaultProfileFactory defaultProfileFactory = new DefaultProfileFactory();

  private StackGresDistributedLogs distributedLogs;

  @Spy
  private StackGresDistributedLogsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresProfile> profileFinder;

  @BeforeEach
  void setUp() {
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    contextAppender = new DistributedLogsInstanceProfileContextAppender(
        profileFinder,
        new DefaultProfileFactory());
  }

  @Test
  void givenDistributedLogsWithProfile_shouldPass() {
    final var profile = Optional.of(
        new StackGresProfileBuilder()
        .withNewSpec()
        .endSpec()
        .build());
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(profile);
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).profile(profile);
  }

  @Test
  void givenDistributedLogsWithoutProfile_shouldFail() {
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex = assertThrows(
        IllegalArgumentException.class,
        () -> contextAppender.appendContext(distributedLogs, contextBuilder));
    assertEquals("SGInstanceProfile size-s was not found", ex.getMessage());
  }

  @Test
  void givenDistributedLogsWithoutDefaultProfile_shouldPass() {
    distributedLogs.getSpec().setSgInstanceProfile(defaultProfileFactory.getDefaultResourceName(distributedLogs));
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).profile(Optional.empty());
  }

}
