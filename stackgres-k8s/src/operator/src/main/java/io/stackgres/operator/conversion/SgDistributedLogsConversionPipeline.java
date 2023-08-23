/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
@Conversion(StackGresDistributedLogs.KIND)
public class SgDistributedLogsConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgDistributedLogsConversionPipeline(
      @Conversion(StackGresDistributedLogs.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().toList();
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }
}
