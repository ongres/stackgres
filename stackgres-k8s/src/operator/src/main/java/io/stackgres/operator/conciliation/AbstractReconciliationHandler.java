/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresContext;

public abstract class AbstractReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T>, ReconciliationOperations {

  private static final String STACKGRES_FIELD_MANAGER = "StackGres";

  protected final KubernetesClient client;

  protected AbstractReconciliationHandler(KubernetesClient client) {
    this.client = client;
  }

  public AbstractReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
  }

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    resource.getMetadata().setManagedFields(null);
    return client.resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(getFieldManager(context, resource))
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public HasMetadata patch(T context, HasMetadata resource, HasMetadata oldResource) {
    resource.getMetadata().setManagedFields(null);
    return client.resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(getFieldManager(context, resource))
            .withForce(true)
            .build(),
            resource);
  }

  private String getFieldManager(T context, HasMetadata resource) {
    if (resource.getApiVersion().startsWith(StackGresContext.STACKGRES_KEY_PREFIX)) {
      return context.getKind();
    }
    return STACKGRES_FIELD_MANAGER;
  }

  @Override
  public HasMetadata replace(T context, HasMetadata resource) {
    return client.resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .update();
  }

  @Override
  public void delete(T context, HasMetadata resource) {
    client.resource(resource).delete();
  }

  @Override
  public void deleteWithOrphans(T context, HasMetadata resource) {
    client.resource(resource)
        .withPropagationPolicy(DeletionPropagation.ORPHAN)
        .delete();
  }

}
