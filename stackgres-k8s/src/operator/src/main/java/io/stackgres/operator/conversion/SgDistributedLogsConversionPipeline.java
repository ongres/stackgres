/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
@Conversion(StackGresDistributedLogs.KIND)
public class SgDistributedLogsConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgDistributedLogsConversionPipeline(
      @Conversion(StackGresDistributedLogs.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }
}
