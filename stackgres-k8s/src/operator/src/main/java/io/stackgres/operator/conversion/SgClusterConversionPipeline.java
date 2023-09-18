/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgcluster.StackGresCluster;

@ApplicationScoped
@Conversion(StackGresCluster.KIND)
public class SgClusterConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgClusterConversionPipeline(
      @Conversion(StackGresCluster.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().toList();
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }

}
