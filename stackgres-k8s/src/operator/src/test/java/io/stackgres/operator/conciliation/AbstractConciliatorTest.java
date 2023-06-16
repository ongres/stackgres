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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.CustomResourceScanner;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractConciliatorTest {

  @Mock
  private CustomResourceScanner<TestResource> scanner;

  @Mock
  private Conciliator<TestResource> conciliator;

  @Mock
  private HandlerDelegator<TestResource> handlerDelegator;

  private TestResource customResource;

  private AbstractReconciliator<TestResource> reconciliator;

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
  void shouldRunOnSingleCustomResourceIfMethodIsCalledTwiceWithSameCustomResourceWhileRunning() {
    CompletableFuture<Void> waitInternal = new CompletableFuture<>();
    CompletableFuture<Void> waitExternal = new CompletableFuture<>();
    doAnswer(invocation -> {
      waitExternal.complete(null);
      waitInternal.join();
      return null;
    }).when(reconciliator).reconciliationCycle(any());

    var testResource1 = new TestResource();
    testResource1.setMetadata(new ObjectMeta());
    testResource1.getMetadata().setName("test1");
    testResource1.getMetadata().setNamespace("test");
    var testResource2 = new TestResource();
    testResource2.setMetadata(new ObjectMeta());
    testResource2.getMetadata().setName("test2");
    testResource2.getMetadata().setNamespace("test");
    reconciliator.reconcile(testResource1);
    waitExternal.join();
    reconciliator.reconcile(testResource1);
    reconciliator.reconcile(testResource1);
    reconciliator.reconcile(testResource2);
    waitInternal.complete(null);

    verify(reconciliator, timeout(1000).times(2)).reconciliationCycle(any());

    reconciliator.stop();

    verify(reconciliator, times(2)).reconciliationCycle(any());
    verify(reconciliator, times(1)).reconciliationCycle(List.of(testResource1));
    verify(reconciliator, times(1)).reconciliationCycle(List.of(testResource1, testResource2));
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

  private AbstractReconciliator<TestResource> buildConciliator() {
    final AbstractReconciliator<TestResource> reconciliator =
        new TestReconciliator(scanner, conciliator, handlerDelegator, null);
    return reconciliator;
  }

  public static class TestReconciliator
      extends AbstractReconciliator<TestResource> {

    TestReconciliator(
        CustomResourceScanner<TestResource> scanner,
        Conciliator<TestResource> conciliator,
        HandlerDelegator<TestResource> handlerDelegator,
        KubernetesClient client) {
      super(scanner, conciliator, handlerDelegator, client, "Test");
    }

    @Override
    public void onPreReconciliation(TestResource config) {
    }

    @Override
    public void onPostReconciliation(TestResource config) {
    }

    @Override
    public void onConfigCreated(TestResource context,
        ReconciliationResult result) {
    }

    @Override
    public void onConfigUpdated(TestResource context,
        ReconciliationResult result) {
    }

    @Override
    public void onError(Exception e, TestResource context) {
    }
  }
}
