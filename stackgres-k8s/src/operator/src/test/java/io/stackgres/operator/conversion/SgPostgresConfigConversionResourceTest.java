/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgPostgresConfigConversionResourceTest
    extends ConversionResourceTest<StackGresPostgresConfig> {

  @Override
  protected StackGresPostgresConfig getCustomResource() {
    return Fixtures.postgresConfig().loadDefault().get();
  }

  @Override
  protected ConversionResource getConversionResource() {
    return new SgPostgresConfigConversionResource(pipeline);
  }
}
