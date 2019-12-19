/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Optional;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource>
    implements DefaultCustomResourceInitializer<T> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractDefaultCustomResourceInitializer.class);

  private KubernetesCustomResourceFinder<T> resourceFinder;

  private CustomResourceScheduler<T> resourceScheduler;

  private DefaultCustomResourceFactory<T> resourceFactory;

  private InitializationQueue queue;

  private void ayncInitialize(T defaultResource) {

    queue.defer(() -> resourceScheduler.create(defaultResource));

  }

  @Override
  public void initialize() {
    T defaultResource = resourceFactory.buildResource();
    String resourceNamespace = defaultResource.getMetadata().getNamespace();
    String resourceName = defaultResource.getMetadata().getName();

    LOGGER.info("Initializing " + resourceName);

    Optional<T> installedResource = resourceFinder
        .findByNameAndNamespace(resourceName, resourceNamespace);

    if (installedResource.isPresent()) {
      LOGGER.info("Default custom resource " + resourceName + " already installed");
    } else {
      LOGGER.info("Installing default custom resource " + resourceName);
      ayncInitialize(defaultResource);
    }

  }

  @Inject
  public void setResourceFinder(KubernetesCustomResourceFinder<T> resourceFinder) {
    this.resourceFinder = resourceFinder;
  }

  @Inject
  public void setResourceScheduler(CustomResourceScheduler<T> resourceScheduler) {
    this.resourceScheduler = resourceScheduler;
  }

  @Inject
  public void setResourceFactory(DefaultCustomResourceFactory<T> resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  @Inject
  public void setQueue(InitializationQueue queue) {
    this.queue = queue;
  }
}
