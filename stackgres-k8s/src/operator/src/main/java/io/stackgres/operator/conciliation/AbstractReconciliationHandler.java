/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.common.resource.ResourceWriter.STACKGRES_FIELD_MANAGER;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.stackgres.common.CdiUtil;

public abstract class AbstractReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T>, ReconciliationOperations {

  private final KubernetesClient client;

  protected AbstractReconciliationHandler(KubernetesClient client) {
    this.client = client;
  }

  public AbstractReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
  }

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    return client.resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public HasMetadata patch(T context, HasMetadata resource, HasMetadata oldResource) {
    return client.resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public HasMetadata replace(T context, HasMetadata resource) {
    return client.resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace();
  }

  @Override
  public void delete(T context, HasMetadata resource) {
    client.resource(resource).delete();
  }

}
