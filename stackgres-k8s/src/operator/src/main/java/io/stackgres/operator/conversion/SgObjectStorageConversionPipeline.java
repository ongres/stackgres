/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
@Conversion(StackGresObjectStorage.KIND)
public class SgObjectStorageConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgObjectStorageConversionPipeline(
      @Conversion(StackGresObjectStorage.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().toList();
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }

}
