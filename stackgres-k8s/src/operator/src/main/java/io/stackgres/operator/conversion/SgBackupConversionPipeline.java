/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
@Conversion(StackGresBackup.KIND)
public class SgBackupConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgBackupConversionPipeline(
      @Conversion(StackGresBackup.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }
}
