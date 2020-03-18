/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource>
    implements DefaultCustomResourceInitializer<T> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractDefaultCustomResourceInitializer.class);

  private CustomResourceFinder<T> resourceFinder;
  private CustomResourceScheduler<T> resourceScheduler;
  private DefaultCustomResourceFactory<T> resourceFactory;

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

  @Inject
  public void setResourceFinder(CustomResourceFinder<T> resourceFinder) {
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

}
