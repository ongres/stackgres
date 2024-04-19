/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FireAndForgetReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(FireAndForgetReconciliationHandler.class);

  private final ReconciliationHandler<T> handler;

  protected FireAndForgetReconciliationHandler(
      ReconciliationHandler<T> handler) {
    this.handler = handler;
  }

  public FireAndForgetReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.handler = null;
  }

  protected abstract boolean canForget(T context, HasMetadata resource);

  @Override
  public final HasMetadata create(T context, HasMetadata resource) {
    return doCreate(context, resource);
  }

  protected HasMetadata doCreate(T context, HasMetadata resource) {
    return handler.create(context, resource);
  }

  @Override
  public final HasMetadata patch(T context, HasMetadata newResource,
      HasMetadata oldResource) {
    if (canForget(context, newResource)) {
      LOGGER.debug("Skipping patching {} {}.{}",
          HasMetadata.getKind(oldResource.getClass()),
          oldResource.getMetadata().getNamespace(),
          oldResource.getMetadata().getName());
      return oldResource;
    }
    return doPatch(context, newResource, oldResource);
  }

  protected HasMetadata doPatch(T context, HasMetadata newResource, HasMetadata oldResource) {
    return handler.patch(context, newResource, oldResource);
  }

  @Override
  public final HasMetadata replace(T context, HasMetadata resource) {
    if (canForget(context, resource)) {
      LOGGER.debug("Skipping replacing {} {}.{}",
          HasMetadata.getKind(resource.getClass()),
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return resource;
    }
    return doReplace(context, resource);
  }

  protected HasMetadata doReplace(T context, HasMetadata resource) {
    return handler.replace(context, resource);
  }

  @Override
  public final void delete(T context, HasMetadata resource) {
    if (canForget(context, resource)) {
      LOGGER.debug("Skipping deleting {} {}.{}",
          HasMetadata.getKind(resource.getClass()),
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return;
    }
    doDelete(context, resource);
  }

  protected void doDelete(T context, HasMetadata resource) {
    handler.delete(context, resource);
  }

  @Override
  public final void deleteWithOrphans(T context, HasMetadata resource) {
    if (canForget(context, resource)) {
      LOGGER.debug("Skipping deleting {} {}.{}",
          HasMetadata.getKind(resource.getClass()),
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return;
    }
    doDeleteWithOrphans(context, resource);
  }

  protected void doDeleteWithOrphans(T context, HasMetadata resource) {
    handler.deleteWithOrphans(context, resource);
  }

}
