/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource>
    implements DefaultCustomResourceInitializer<T> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractDefaultCustomResourceInitializer.class);

  private final CustomResourceFinder<T> resourceFinder;
  private final CustomResourceScheduler<T> resourceScheduler;
  private final DefaultCustomResourceFactory<T> resourceFactory;

  public AbstractDefaultCustomResourceInitializer(CustomResourceFinder<T> resourceFinder,
      CustomResourceScheduler<T> resourceScheduler,
      DefaultCustomResourceFactory<T> resourceFactory) {
    super();
    this.resourceFinder = resourceFinder;
    this.resourceScheduler = resourceScheduler;
    this.resourceFactory = resourceFactory;
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
      resourceScheduler.create(defaultResource);
    }
  }

}
