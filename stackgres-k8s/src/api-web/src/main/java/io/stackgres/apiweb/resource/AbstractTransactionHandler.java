/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.resource.ResourceWriter;

public abstract class AbstractTransactionHandler<R extends HasMetadata>
    implements ResourceTransactionHandler<R> {

  private ResourceWriter<R> writer;

  @Override
  public void create(R resource, Runnable transaction) {

    writer.create(resource);
    try {
      transaction.run();
    } catch (Exception exception) {
      rollBack(exception, resource);
    }
  }

  @Override
  public void rollBack(Exception failure, R resource) {
    try {
      writer.delete(resource);
    } catch (Exception exception) {
      failure.addSuppressed(exception);
    }
    throw new RuntimeException(failure);
  }

  @Inject
  public void setWriter(ResourceWriter<R> writer) {
    this.writer = writer;
  }
}
