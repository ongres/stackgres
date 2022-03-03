/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;

@ApplicationScoped
public class ObjectStorageFactoryProvider
    implements DefaultFactoryProvider<DefaultCustomResourceFactory<StackGresObjectStorage>> {

  private final DefaultCustomResourceFactory<StackGresObjectStorage> factory;

  @Inject
  public ObjectStorageFactoryProvider(
      DefaultCustomResourceFactory<StackGresObjectStorage> factory) {
    this.factory = factory;
  }

  @Override
  public List<DefaultCustomResourceFactory<StackGresObjectStorage>> getFactories() {
    return List.of(factory);
  }
}
