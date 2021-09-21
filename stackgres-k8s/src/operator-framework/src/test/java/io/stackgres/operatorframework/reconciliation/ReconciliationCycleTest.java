/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle.ReconciliationCycleResult;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationCycleTest {

  @Mock
  private KubernetesClient client;

  @Mock
  private Reconciliator<ResourceHandlerContext> reconciliator;

  @Mock
  private ResourceHandlerSelector<ResourceHandlerContext> handlerSelector;

  @Mock
  private ReconciliationCycle<ResourceHandlerContext, CustomResource<?, ?>,
      ResourceHandlerSelector<ResourceHandlerContext>> mockReconciliationCycle;

  private ReconciliationCycle<?, ?, ?> reconciliationCycle;

  private CustomResource<?, ?> resource;

  @Mock
  private ResourceHandlerContext context;

  @BeforeEach
  void setUp() throws Exception {
    reconciliationCycle = new TestReconciliationCycle(ReconciliationCycleTest.this.client,
        ReconciliationCycleTest.this.reconciliator, ReconciliationCycleTest.this.handlerSelector);
    when(mockReconciliationCycle.getContextFromResource(any()))
        .thenReturn(context);
    when(mockReconciliationCycle.getRequiredResources(any()))
        .thenReturn(ImmutableList.of());
    resource = new TestCustomResource();
  }

  @Test
  void ifCalled_reconciliationShouldWork() throws Exception {
    when(mockReconciliationCycle.getExistingContextResources())
        .thenReturn(ImmutableList.of(resource));
    when(reconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    ReconciliationCycleResult<?> result = reconciliationCycle.reconciliationCycle();
    result.throwIfFailed();
    verify(reconciliator, times(1)).reconcile(any(), any());
  }

  @Test
  void ifResourceReconciliationFail_reconciliationShouldContinue() throws Exception {
    when(mockReconciliationCycle.getExistingContextResources())
        .thenReturn(ImmutableList.of(resource, resource));
    when(reconciliator.reconcile(any(), any()))
        .thenThrow(RuntimeException.class)
        .thenReturn(new ReconciliationResult<>());
    ReconciliationCycleResult<?> result = reconciliationCycle.reconciliationCycle();
    Assertions.assertFalse(result.success());
    Assertions.assertEquals(Optional.empty(), result.getException());
    Assertions.assertEquals(1, result.getContextExceptions().size());
    verify(reconciliator, times(2)).reconcile(any(), any());
  }

  class TestReconciliationCycle extends
      ReconciliationCycle<ResourceHandlerContext, CustomResource<?, ?>,
      ResourceHandlerSelector<ResourceHandlerContext>> {

    public TestReconciliationCycle(KubernetesClient client,
        Reconciliator<ResourceHandlerContext> reconciliator,
        ResourceHandlerSelector<ResourceHandlerContext> handlerSelector) {
      super("Test", client, reconciliator, handlerSelector);
    }

    @Override
    protected void onError(Exception ex) {
      mockReconciliationCycle.onError(ex);
    }

    @Override
    protected void onConfigError(ResourceHandlerContext context, HasMetadata contextResource,
        Exception ex) {
      mockReconciliationCycle.onConfigError(context, contextResource, ex);
    }

    @Override
    protected ResourceHandlerContext getContextWithExistingResourcesOnly(
        ResourceHandlerContext context,
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
      return mockReconciliationCycle.getContextWithExistingResourcesOnly(context,
          existingResourcesOnly);
    }

    @Override
    protected ImmutableList<HasMetadata> getRequiredResources(ResourceHandlerContext context) {
      return mockReconciliationCycle.getRequiredResources(context);
    }

    @Override
    protected ResourceHandlerContext getContextWithExistingAndRequiredResources(
        ResourceHandlerContext context,
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
      return mockReconciliationCycle.getContextWithExistingAndRequiredResources(context,
          requiredResources, existingResources);
    }

    @Override
    protected ImmutableList<CustomResource<?, ?>> getExistingContextResources() {
      return mockReconciliationCycle.getExistingContextResources();
    }

    @Override
    protected ResourceHandlerContext getContextFromResource(
        CustomResource<?, ?> contextResource) {
      return mockReconciliationCycle.getContextFromResource(contextResource);
    }
  }

  @Group("test")
  @Version("1")
  static class TestCustomResource extends CustomResource<Object, Object> {
    private static final long serialVersionUID = 1L;

    public TestCustomResource() {
      super();
      getMetadata().setNamespace("test");
      getMetadata().setName("test");
    }
  }
}
