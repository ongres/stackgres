/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.CustomResourceScanner;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomResourceConciliatorTest {

  @Mock
  private CustomResourceScanner<CustomResource<Object, Object>> scanner;

  @Mock
  private Conciliator<CustomResource<Object, Object>> conciliator;

  @Mock
  private HandlerDelegator<CustomResource<Object, Object>> handlerDelegator;

  private CustomResource<Object, Object> customResource;

  private AbstractReconciliator<CustomResource<Object, Object>> reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = spy(buildConciliator());
    reconciliator.start();
    customResource = new TestResource();
    customResource.setMetadata(new ObjectMeta());
    customResource.getMetadata().setName("test");
    customResource.getMetadata().setNamespace("test-namespace");
    customResource.getMetadata().setUid("1");
  }

  @Test
  void shouldNotRunReconciliationIfReconciliationMethodIsNotCalled() throws Exception {
    Thread.sleep(1000);

    verify(reconciliator, times(0)).reconciliationCycle(List.of());

    reconciliator.stop();

    verify(reconciliator, times(0)).reconciliationCycle(any());
    verify(reconciliator, times(0)).onPreReconciliation(any());
    verify(reconciliator, times(0)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldRunReconciliationOnceIfReconciliationMethodIsCalledOnce() {
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(),
            List.of()));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(any());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldRunReconciliationOnceIfReconciliationMethodIsCalledTwiceWhileRunning() {
    CompletableFuture<Void> waitInternal = new CompletableFuture<>();
    CompletableFuture<Void> waitExternal = new CompletableFuture<>();
    doAnswer(invocation -> {
      waitExternal.complete(null);
      waitInternal.join();
      return null;
    }).when(reconciliator).reconciliationCycle(any());

    reconciliator.reconcileAll();
    waitExternal.join();
    reconciliator.reconcileAll();
    reconciliator.reconcileAll();
    waitInternal.complete(null);

    verify(reconciliator, timeout(1000).times(2)).reconciliationCycle(List.of());

    reconciliator.stop();

    verify(reconciliator, times(2)).reconciliationCycle(any());
  }

  @Test
  void shouldCallOnErrorOnceIfReconciliationMethodIsCalledOnceAndThrowsError() {
    when(conciliator.evalReconciliationState(any()))
        .thenThrow(RuntimeException.class);

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(List.of(customResource));

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(0)).onPostReconciliation(any());
    verify(reconciliator, times(1)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnPostReconciliationIfReconciliationMethodIsCalledOnce() {
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(),
            List.of()));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(List.of(customResource));

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnConfigCreatedIfReconciliationMethodIsCalledOnce() {
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(
                new ConfigMapBuilder()
                .withNewMetadata()
                .withNamespace("test-namespace")
                .withName("test")
                .endMetadata()
                .build()),
            List.of(),
            List.of()));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(eq(List.of(customResource)));

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(1)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnConfigUpdatedIfReconciliationMethodIsCalledOnce() {
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(Tuple.tuple(
                new ConfigMapBuilder()
                .withNewMetadata()
                .withNamespace("test-namespace")
                .withName("test")
                .endMetadata()
                .build(),
                new ConfigMapBuilder()
                .withNewMetadata()
                .withNamespace("test-namespace")
                .withName("test")
                .endMetadata()
                .build())),
            List.of()));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(List.of(customResource));

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(1)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnConfigUpdatedIfReconciliationMethodIsCalledOnceHavingDeletions() {
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(),
            List.of(
                new ConfigMapBuilder()
                .withNewMetadata()
                .withNamespace("test-namespace")
                .withName("test")
                .endMetadata()
                .build())));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(List.of(customResource));

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(1)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallScannerIfReconciliationAllMethodIsCalled() {
    reconciliator.reconcileAll();

    verify(scanner, times(1)).getResources();

    verify(reconciliator, timeout(1000).times(1)).reconciliationCycle(List.of());
  }

  private AbstractReconciliator<CustomResource<Object, Object>> buildConciliator() {
    final AbstractReconciliator<CustomResource<Object, Object>> reconciliator =
        new TestReconciliator(scanner, conciliator, handlerDelegator, null);
    return reconciliator;
  }

  public static class TestReconciliator
      extends AbstractReconciliator<CustomResource<Object, Object>> {

    TestReconciliator(
        CustomResourceScanner<CustomResource<Object, Object>> scanner,
        Conciliator<CustomResource<Object, Object>> conciliator,
        HandlerDelegator<CustomResource<Object, Object>> handlerDelegator,
        KubernetesClient client) {
      super(scanner, conciliator, handlerDelegator, client, "Test");
    }

    @Override
    public void onPreReconciliation(CustomResource<Object, Object> config) {
    }

    @Override
    public void onPostReconciliation(CustomResource<Object, Object> config) {
    }

    @Override
    public void onConfigCreated(CustomResource<Object, Object> context,
        ReconciliationResult result) {
    }

    @Override
    public void onConfigUpdated(CustomResource<Object, Object> context,
        ReconciliationResult result) {
    }

    @Override
    public void onError(Exception e, CustomResource<Object, Object> context) {
    }
  }
}
