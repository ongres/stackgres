/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.CustomResourceFinder;
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
  private CustomResourceFinder<TestResource> finder;

  @Mock
  private AbstractConciliator<TestResource> conciliator;

  @Mock
  private DeployedResourcesCache deployedResourcesCache;

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

    verify(reconciliator, times(0)).reconciliationsCycle(List.of());

    reconciliator.stop();

    verify(reconciliator, times(0)).reconciliationsCycle(any());
    verify(reconciliator, times(0)).onPreReconciliation(any());
    verify(reconciliator, times(0)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldRunReconciliationOnceIfReconciliationMethodIsCalledOnce() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(),
            List.of()));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(any());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldRunReconciliationOnceIfReconciliationMethodIsCalledTwiceWhileRunning() {
    when(scanner.getResources()).thenReturn(List.of(customResource));
    CompletableFuture<Void> waitInternal = new CompletableFuture<>();
    CompletableFuture<Void> waitExternal = new CompletableFuture<>();
    doAnswer(invocation -> {
      waitExternal.complete(null);
      waitInternal.join();
      return null;
    }).when(reconciliator).reconciliationCycle(any(), anyBoolean());

    reconciliator.reconcileAll();
    waitExternal.join();
    reconciliator.reconcileAll();
    reconciliator.reconcileAll();
    waitInternal.complete(null);

    verify(reconciliator, timeout(1000).times(2)).reconciliationsCycle(any());
    verify(reconciliator, timeout(1000).times(2)).reconciliationCycle(any(), anyBoolean());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(
        List.of(Optional.empty()));
    verify(reconciliator, times(1)).reconciliationsCycle(
        List.of(Optional.empty(), Optional.empty()));
    verify(reconciliator, times(2)).reconciliationCycle(customResource, false);
  }

  @Test
  void shouldRunOnSingleCustomResourceIfMethodIsCalledTwiceWithSameCustomResourceWhileRunning() {
    CompletableFuture<Void> waitInternal = new CompletableFuture<>();
    CompletableFuture<Void> waitExternal = new CompletableFuture<>();
    doAnswer(invocation -> {
      waitExternal.complete(null);
      waitInternal.join();
      return null;
    }).when(reconciliator).reconciliationsCycle(any());

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

    verify(reconciliator, timeout(1000).times(2)).reconciliationsCycle(any());

    reconciliator.stop();

    verify(reconciliator, times(2)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).reconciliationsCycle(List.of(testResource1)
        .stream().map(Optional::of).toList());
    verify(reconciliator, times(1)).reconciliationsCycle(
        List.of(testResource1, testResource1, testResource2)
        .stream().map(Optional::of).toList());
  }

  @Test
  void shouldCallOnErrorOnceIfReconciliationMethodIsCalledOnceAndThrowsError() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
    when(conciliator.evalReconciliationState(any()))
        .thenThrow(RuntimeException.class);

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(List.of(customResource)
        .stream().map(Optional::of).toList());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(0)).onPostReconciliation(any());
    verify(reconciliator, times(1)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnPostReconciliationIfReconciliationMethodIsCalledOnce() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(),
            List.of()));

    reconciliator.reconcile(customResource);

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(List.of(customResource)
        .stream().map(Optional::of).toList());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnConfigCreatedIfReconciliationMethodIsCalledOnce() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
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

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(eq(List.of(customResource)
        .stream().map(Optional::of).toList()));

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(1)).onConfigCreated(any(), any());
    verify(reconciliator, times(0)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnConfigUpdatedIfReconciliationMethodIsCalledOnce() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
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

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(List.of(customResource)
        .stream().map(Optional::of).toList());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(1)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallOnConfigUpdatedIfReconciliationMethodIsCalledOnceHavingDeletions() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
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

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(List.of(customResource)
        .stream().map(Optional::of).toList());

    reconciliator.stop();

    verify(reconciliator, times(1)).reconciliationsCycle(any());
    verify(reconciliator, times(1)).onPreReconciliation(any());
    verify(reconciliator, times(1)).onPostReconciliation(any());
    verify(reconciliator, times(0)).onError(any(), any());
    verify(reconciliator, times(0)).onConfigCreated(any(), any());
    verify(reconciliator, times(1)).onConfigUpdated(any(), any());
  }

  @Test
  void shouldCallScannerIfReconciliationAllMethodIsCalled() {
    reconciliator.reconciliationsCycle(List.of(Optional.empty()));

    verify(scanner, times(1)).getResources();

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(any());
  }

  @Test
  void shouldCallFinderIfReconciliationMethodIsCalledForOneResource() {
    when(finder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(customResource));
    when(conciliator.evalReconciliationState(any()))
        .thenReturn(new ReconciliationResult(
            List.of(),
            List.of(),
            List.of()));
    reconciliator.reconciliationsCycle(List.of(Optional.of(customResource)));

    verify(finder, times(1)).findByNameAndNamespace(any(), any());

    verify(reconciliator, timeout(1000).times(1)).reconciliationsCycle(any());
  }

  private AbstractReconciliator<TestResource> buildConciliator() {
    final AbstractReconciliator<TestResource> reconciliator =
        new TestReconciliator(scanner, finder, conciliator, deployedResourcesCache,
            handlerDelegator, null);
    return reconciliator;
  }

  public static class TestReconciliator
      extends AbstractReconciliator<TestResource> {

    TestReconciliator(
        CustomResourceScanner<TestResource> scanner,
        CustomResourceFinder<TestResource> finder,
        AbstractConciliator<TestResource> conciliator,
        DeployedResourcesCache deployedResourcesCache,
        HandlerDelegator<TestResource> handlerDelegator,
        KubernetesClient client) {
      super(scanner, finder, conciliator, deployedResourcesCache,
          handlerDelegator, client, "Test");
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
