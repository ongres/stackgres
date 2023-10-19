/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgInstanceProfileConversionResourceTest extends ConversionResourceTest<StackGresProfile> {

  @Override
  protected StackGresProfile getCustomResource() {
    return Fixtures.instanceProfile().loadSizeM().get();
  }

  @Override
  protected ConversionResource getConversionResource() {
    return new SgDistributedLogsConversionResource(pipeline);
  }
}
