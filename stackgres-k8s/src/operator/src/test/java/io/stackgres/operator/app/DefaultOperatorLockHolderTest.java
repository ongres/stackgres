/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static io.stackgres.common.StackGresContext.LOCK_POD_KEY;
import static io.stackgres.common.StackGresContext.LOCK_SERVICE_ACCOUNT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultOperatorLockHolderTest {

  private static final String OPERATOR_POD_NAME = "test-pod";

  private static final String OPERATOR_SERVICE_ACCOUNT = "test-account";

  private static final int LOCK_DURATION = 2;

  private static final int LOCK_POLL_INTERVAL = 1;

  @Mock
  private CustomResourceScanner<StackGresConfig> scanner;

  @Mock
  private CustomResourceScheduler<StackGresConfig> scheduler;

  @Mock
  private OperatorPropertyContext context;

  @Mock
  private AbstractReconciliator<?> reconciliator;

  private DefaultOperatorLockHolder operatorLockHolder;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    operatorLockHolder = new DefaultOperatorLockHolder(scanner, scheduler, context);
    operatorLockHolder.startReconciliation();
    operatorLockHolder.register(reconciliator);
    config = Fixtures.config().loadDefault().get();
    when(context.getInt(OperatorProperty.LOCK_POLL_INTERVAL)).thenReturn(LOCK_POLL_INTERVAL);
    when(context.getInt(OperatorProperty.LOCK_DURATION)).thenReturn(LOCK_DURATION);
    when(context.getString(OperatorProperty.OPERATOR_SERVICE_ACCOUNT))
        .thenReturn(OPERATOR_SERVICE_ACCOUNT);
    when(context.getString(OperatorProperty.OPERATOR_POD_NAME))
        .thenReturn(OPERATOR_POD_NAME);
  }

  @AfterEach
  void tearDown() {
    operatorLockHolder.stop();
  }

  @Test
  void shouldLockConfigWhenUnlocked() throws Exception {
    CompletableFuture<StackGresConfig> updatedConfigFuture = new CompletableFuture<>();
    final long checkTimestamp = System.currentTimeMillis() / 1000;
    when(scanner.getResources()).thenReturn(List.of(config));
    when(scheduler.update(any(), any())).then(invocation -> {
      @SuppressWarnings("unchecked")
      var consumer = (Consumer<StackGresConfig>) invocation.getArgument(1);
      StackGresConfig foundConfig = JsonUtil.copy(config);
      consumer.accept(foundConfig);
      updatedConfigFuture.complete(foundConfig);
      return foundConfig;
    });
    operatorLockHolder.start();
    StackGresConfig updatedConfig = updatedConfigFuture.get(200, TimeUnit.MILLISECONDS);
    assertEquals(
        OPERATOR_POD_NAME,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertEquals(
        OPERATOR_SERVICE_ACCOUNT,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_SERVICE_ACCOUNT_KEY));
    assertTrue(StackGresUtil.isLockedBy(updatedConfig, OPERATOR_POD_NAME, checkTimestamp));
    assertTrue(operatorLockHolder.isLeader());
    verify(reconciliator, Mockito.times(1)).reconcileAll();
  }

  @Test
  void shouldDoNothingWhenAlreadyLocked() throws Exception {
    AtomicInteger calls = new AtomicInteger(0);
    CompletableFuture<StackGresConfig> updatedConfigFuture = new CompletableFuture<>();
    final long checkTimestamp = System.currentTimeMillis() / 1000;
    StackGresUtil.setLock(config, OPERATOR_SERVICE_ACCOUNT, OPERATOR_POD_NAME, LOCK_DURATION);
    when(scanner.getResources()).thenReturn(List.of(config));
    when(scheduler.update(any(), any())).then(invocation -> {
      @SuppressWarnings("unchecked")
      var consumer = (Consumer<StackGresConfig>) invocation.getArgument(1);
      StackGresConfig foundConfig = JsonUtil.copy(config);
      consumer.accept(foundConfig);
      int currentCalls = calls.incrementAndGet();
      if (currentCalls == 2) {
        updatedConfigFuture.complete(foundConfig);
      }
      return foundConfig;
    });
    operatorLockHolder.start();
    StackGresConfig updatedConfig = updatedConfigFuture.get(
        LOCK_POLL_INTERVAL * 1000 + 200, TimeUnit.MILLISECONDS);
    assertEquals(
        OPERATOR_POD_NAME,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertEquals(
        OPERATOR_SERVICE_ACCOUNT,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_SERVICE_ACCOUNT_KEY));
    assertTrue(StackGresUtil.isLockedBy(updatedConfig, OPERATOR_POD_NAME, checkTimestamp));
    assertTrue(operatorLockHolder.isLeader());
    verify(reconciliator, Mockito.atLeast(1)).reconcileAll();
    verify(reconciliator, Mockito.atMost(2)).reconcileAll();
  }

  @Test
  void shouldLockConfigWhenLockLost() throws Exception {
    AtomicInteger calls = new AtomicInteger(0);
    CompletableFuture<StackGresConfig> lockAcquiredConfigFuture = new CompletableFuture<>();
    CompletableFuture<StackGresConfig> updatedConfigFuture = new CompletableFuture<>();
    final long checkTimestamp = System.currentTimeMillis() / 1000;
    StackGresUtil.setLock(config, OPERATOR_SERVICE_ACCOUNT, OPERATOR_POD_NAME, LOCK_DURATION);
    when(scanner.getResources()).thenReturn(List.of(config));
    when(scheduler.update(any(), any())).then(invocation -> {
      @SuppressWarnings("unchecked")
      var consumer = (Consumer<StackGresConfig>) invocation.getArgument(1);
      StackGresConfig foundConfig = JsonUtil.copy(config);
      int currentCalls = calls.incrementAndGet();
      if (currentCalls == 1) {
        consumer.accept(foundConfig);
        lockAcquiredConfigFuture.complete(foundConfig);
      }
      if (currentCalls == 2) {
        StackGresUtil.setLock(foundConfig, OPERATOR_SERVICE_ACCOUNT, OPERATOR_POD_NAME,
            LOCK_DURATION, checkTimestamp - 2 * LOCK_DURATION);
        consumer.accept(foundConfig);
        updatedConfigFuture.complete(foundConfig);
      }
      return foundConfig;
    });
    operatorLockHolder.start();
    StackGresConfig lockAcquiredConfig = lockAcquiredConfigFuture.get(
        200, TimeUnit.MILLISECONDS);
    assertEquals(
        OPERATOR_POD_NAME,
        lockAcquiredConfig.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertEquals(
        OPERATOR_SERVICE_ACCOUNT,
        lockAcquiredConfig.getMetadata().getAnnotations().get(LOCK_SERVICE_ACCOUNT_KEY));
    assertTrue(StackGresUtil.isLockedBy(lockAcquiredConfig, OPERATOR_POD_NAME, checkTimestamp));
    assertTrue(operatorLockHolder.isLeader());
    verify(reconciliator, Mockito.times(1)).reconcileAll();
    StackGresConfig updatedConfig = updatedConfigFuture.get(
        LOCK_POLL_INTERVAL * 1000 + 200, TimeUnit.MILLISECONDS);
    assertEquals(
        OPERATOR_POD_NAME,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertEquals(
        OPERATOR_SERVICE_ACCOUNT,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_SERVICE_ACCOUNT_KEY));
    assertTrue(StackGresUtil.isLockedBy(updatedConfig, OPERATOR_POD_NAME, checkTimestamp));
    assertTrue(operatorLockHolder.isLeader());
    verify(reconciliator, Mockito.times(2)).reconcileAll();
  }

  @Test
  void shouldWaitWhenLockedByAnother() throws Exception {
    CompletableFuture<StackGresConfig> updatedConfigFuture = new CompletableFuture<>();
    final long checkTimestamp = System.currentTimeMillis() / 1000;
    StackGresUtil.setLock(config, OPERATOR_SERVICE_ACCOUNT, "another-test-pod", LOCK_DURATION);
    when(scanner.getResources()).thenReturn(List.of(config));
    when(scheduler.update(any(), any())).then(invocation -> {
      @SuppressWarnings("unchecked")
      var consumer = (Consumer<StackGresConfig>) invocation.getArgument(1);
      StackGresConfig foundConfig = JsonUtil.copy(config);
      consumer.accept(foundConfig);
      updatedConfigFuture.complete(foundConfig);
      return foundConfig;
    });
    operatorLockHolder.start();
    StackGresConfig updatedConfig = updatedConfigFuture.get(200, TimeUnit.MILLISECONDS);
    assertEquals(
        "another-test-pod",
        updatedConfig.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertEquals(
        OPERATOR_SERVICE_ACCOUNT,
        updatedConfig.getMetadata().getAnnotations().get(LOCK_SERVICE_ACCOUNT_KEY));
    assertTrue(StackGresUtil.isLockedBy(updatedConfig, "another-test-pod", checkTimestamp));
    assertFalse(operatorLockHolder.isLeader());
    verify(reconciliator, Mockito.times(0)).reconcileAll();
  }

}
