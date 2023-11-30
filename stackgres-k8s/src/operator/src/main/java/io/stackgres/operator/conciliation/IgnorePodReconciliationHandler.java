/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IgnorePodReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(IgnorePodReconciliationHandler.class);

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    LOGGER.debug("Skipping creating Pod {}.{}",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    return resource;
  }

  @Override
  public HasMetadata patch(T context, HasMetadata newResource,
      HasMetadata oldResource) {
    LOGGER.debug("Skipping patching Pod {}.{}",
        oldResource.getMetadata().getNamespace(),
        oldResource.getMetadata().getName());
    return oldResource;
  }

  @Override
  public HasMetadata replace(T context, HasMetadata resource) {
    LOGGER.warn("Skipping replacing Pod {}.{}",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    return resource;
  }

  @Override
  public void delete(T context, HasMetadata resource) {
    LOGGER.debug("Skipping deleting Pod {}.{}",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

  @Override
  public void deleteWithOrphans(T context, HasMetadata resource) {
    LOGGER.debug("Skipping deleting Pod {}.{}",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

}
