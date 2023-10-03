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

  private ReconciliationCycle<?, TestCustomResource, ?> reconciliationCycle;

  private TestCustomResource resource;
  private TestCustomResource resource1;

  @Mock
  private ResourceHandlerContext context;

  @BeforeEach
  void setUp() throws Exception {
    reconciliationCycle = new TestReconciliationCycle(ReconciliationCycleTest.this.client,
        ReconciliationCycleTest.this.reconciliator, ReconciliationCycleTest.this.handlerSelector);
    resource = new TestCustomResource("test");
    resource1 = new TestCustomResource("test1");
  }

  @Test
  void ifCalled_reconciliationShouldWork() throws Exception {
    when(reconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>())
        .thenThrow(RuntimeException.class)
        .thenReturn(new ReconciliationResult<>());
    ReconciliationCycleResult<?> result =
        reconciliationCycle.reconciliationCycle(ImmutableList.of(Optional.of(resource)));
    result.throwIfFailed();
    verify(reconciliator, times(1)).reconcile(any(), any());
  }

  @Test
  void ifResourceReconciliationFail_reconciliationShouldContinue() throws Exception {
    when(reconciliator.reconcile(any(), any()))
        .thenThrow(RuntimeException.class)
        .thenReturn(new ReconciliationResult<>());
    ReconciliationCycleResult<?> result =
        reconciliationCycle.reconciliationCycle(
            ImmutableList.of(Optional.of(resource), Optional.of(resource1)));
    Assertions.assertFalse(result.success());
    Assertions.assertEquals(Optional.empty(), result.getException());
    Assertions.assertEquals(1, result.getContextExceptions().size());
    verify(reconciliator, times(2)).reconcile(any(), any());
  }

  class TestReconciliationCycle extends
      ReconciliationCycle<ResourceHandlerContext, TestCustomResource,
      ResourceHandlerSelector<ResourceHandlerContext>> {

    public TestReconciliationCycle(KubernetesClient client,
        Reconciliator<ResourceHandlerContext> reconciliator,
        ResourceHandlerSelector<ResourceHandlerContext> handlerSelector) {
      super("Test", client, reconciliator, handlerSelector);
    }

    @Override
    protected void onError(Exception ex) {
    }

    @Override
    protected void onConfigError(ResourceHandlerContext context, HasMetadata contextResource,
        Exception ex) {
    }

    @Override
    protected ResourceHandlerContext getContextWithExistingResourcesOnly(
        ResourceHandlerContext context,
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
      return null;
    }

    @Override
    protected ImmutableList<HasMetadata> getRequiredResources(ResourceHandlerContext context) {
      return ImmutableList.of();
    }

    @Override
    protected ResourceHandlerContext getContextWithExistingAndRequiredResources(
        ResourceHandlerContext context,
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
      return null;
    }

    @Override
    protected ImmutableList<TestCustomResource> getExistingContextResources() {
      return null;
    }

    @Override
    protected TestCustomResource getExistingContextResource(TestCustomResource contextResource) {
      return contextResource;
    }

    @Override
    protected ResourceHandlerContext getContextFromResource(TestCustomResource contextResource) {
      return null;
    }
  }

  @Group("test")
  @Version("1")
  static class TestCustomResource extends CustomResource<Object, Object> {
    private static final long serialVersionUID = 1L;

    public TestCustomResource(String name) {
      super();
      getMetadata().setNamespace("test");
      getMetadata().setName(name);
    }
  }
}
