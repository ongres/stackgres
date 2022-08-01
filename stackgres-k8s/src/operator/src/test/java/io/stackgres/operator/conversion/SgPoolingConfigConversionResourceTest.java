/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgPoolingConfigConversionResourceTest
    extends ConversionResourceTest<StackGresPoolingConfig> {

  @Override
  protected StackGresPoolingConfig getCustomResource() {
    return Fixtures.poolingConfig().loadDefault().get();
  }

  @Override
  protected ConversionResource getConversionResource() {
    return new SgPoolingConfigConversionResource(pipeline);
  }
}
