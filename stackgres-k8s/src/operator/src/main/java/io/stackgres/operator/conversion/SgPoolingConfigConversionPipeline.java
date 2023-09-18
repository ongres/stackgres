/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;

@ApplicationScoped
@Conversion(StackGresPoolingConfig.KIND)
public class SgPoolingConfigConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgPoolingConfigConversionPipeline(
      @Conversion(StackGresPoolingConfig.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().toList();
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }
}
