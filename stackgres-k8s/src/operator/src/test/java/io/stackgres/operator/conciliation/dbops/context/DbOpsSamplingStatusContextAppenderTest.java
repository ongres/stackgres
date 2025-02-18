/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatusBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsSamplingStatusContextAppenderTest {

  private DbOpsSamplingStatusContextAppender contextAppender;

  private StackGresDbOps dbOps;

  private StackGresDbOps sampling;

  @Spy
  private StackGresDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.dbOps().loadPgbench().get();
    sampling = Fixtures.dbOps().loadSampling().get();
    dbOps.getSpec().getBenchmark().getPgbench()
        .setSamplingSgDbOps(sampling.getMetadata().getName());
    contextAppender = new DbOpsSamplingStatusContextAppender(
        dbOpsFinder);
  }

  @Test
  void givenDbOpsWithSamplingDbOps_shouldPass() {
    when(dbOpsFinder.findByNameAndNamespace(
        dbOps.getSpec().getBenchmark().getPgbench().getSamplingSgDbOps(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(sampling));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).samplingStatus(
        Optional.of(sampling.getStatus().getBenchmark().getSampling()));
  }

  @Test
  void givenDbOpsNotUsingSamplingDbOps_shouldPass() {
    dbOps.getSpec().getBenchmark().getPgbench().setSamplingSgDbOps(null);
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).samplingStatus(Optional.empty());
  }

  @Test
  void givenDbOpsWithoutSamplingDbOps_shouldFail() {
    when(dbOpsFinder.findByNameAndNamespace(
        dbOps.getSpec().getBenchmark().getPgbench().getSamplingSgDbOps(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGDbOps stackgres was not found or has no has no sampling status", ex.getMessage());
  }

  @Test
  void givenDbOpsWithSamplingDbOpsWithoutStatus_shouldFail() {
    sampling.getStatus().getBenchmark().setSampling(null);
    when(dbOpsFinder.findByNameAndNamespace(
        dbOps.getSpec().getBenchmark().getPgbench().getSamplingSgDbOps(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGDbOps stackgres was not found or has no has no sampling status", ex.getMessage());
  }

  @Test
  void givenCompletedDbOpsWithoutSamplingDbOps_shouldPass() {
    dbOps.setStatus(
        new StackGresDbOpsStatusBuilder()
        .withConditions(DbOpsStatusCondition.DBOPS_COMPLETED.getCondition())
        .build());
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).samplingStatus(Optional.empty());
    verify(dbOpsFinder, Mockito.never()).findByNameAndNamespace(
        dbOps.getSpec().getBenchmark().getPgbench().getSamplingSgDbOps(),
        dbOps.getMetadata().getNamespace());
  }

}
