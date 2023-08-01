/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;

@ApplicationScoped
@Conversion(StackGresCluster.KIND)
public class SgConfigConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgConfigConversionPipeline(
      @Conversion(StackGresConfig.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }

}
