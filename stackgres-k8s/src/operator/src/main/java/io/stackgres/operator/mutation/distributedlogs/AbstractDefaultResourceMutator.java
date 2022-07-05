/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;

public abstract class AbstractDefaultResourceMutator<T extends CustomResource<?, ?>>
    implements DistributedLogsMutator {

  @Inject
  DefaultCustomResourceFactory<T> resourceFactory;

  @Inject
  CustomResourceFinder<T> finder;

  @Inject
  CustomResourceScheduler<T> scheduler;

  private transient JsonPointer targetPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    targetPointer = getTargetPointer();
  }

  protected List<JsonPatchOperation> addDefaultResource(StackGresDistributedLogsReview review) {
    T defaultResource = resourceFactory.buildResource();

    StackGresDistributedLogs targetDistributedLogs = review.getRequest().getObject();
    String targetNamespace = targetDistributedLogs.getMetadata().getNamespace();

    String defaultResourceName = defaultResource.getMetadata().getName();

    if (applyDefault(targetDistributedLogs)) {

      if (!finder.findByNameAndNamespace(defaultResourceName, targetNamespace).isPresent()) {
        defaultResource.getMetadata().setNamespace(targetNamespace);
        scheduler.create(defaultResource);
      }

      return Collections.singletonList(
          buildAddOperation(targetPointer, defaultResourceName));
    }

    return List.of();
  }

  protected boolean applyDefault(StackGresDistributedLogs targetDistributedLogs) {
    return isTargetPropertyEmpty(targetDistributedLogs);
  }

  protected boolean isTargetPropertyEmpty(StackGresDistributedLogs targetDistributedLogs) {
    return isEmpty(getTargetPropertyValue(targetDistributedLogs));
  }

  protected abstract String getTargetPropertyValue(StackGresDistributedLogs targetDistributedLogs);

  protected abstract JsonPointer getTargetPointer() throws NoSuchFieldException;

  public void setResourceFactory(DefaultCustomResourceFactory<T> resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  public void setFinder(CustomResourceFinder<T> finder) {
    this.finder = finder;
  }

  public void setScheduler(CustomResourceScheduler<T> scheduler) {
    this.scheduler = scheduler;
  }
}
