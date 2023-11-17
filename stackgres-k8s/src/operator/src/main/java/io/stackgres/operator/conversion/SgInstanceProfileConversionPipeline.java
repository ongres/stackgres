/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
@Conversion(StackGresProfile.KIND)
public class SgInstanceProfileConversionPipeline implements ConversionPipeline {

  private final List<Converter> converters;

  @Inject
  public SgInstanceProfileConversionPipeline(
      @Conversion(StackGresProfile.KIND) Instance<Converter> converters) {
    this.converters = converters.stream().toList();
  }

  @Override
  public List<Converter> getConverters() {
    return converters;
  }
}
