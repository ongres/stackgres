/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgDistributedLogsConversionResourceTest
    extends ConversionResourceTest<StackGresDistributedLogs> {

  @Override
  protected StackGresDistributedLogs getCustomResource() {
    return Fixtures.distributedLogs().loadDefault().get();
  }

  @Override
  protected ConversionResource getConversionResource() {
    return new SgDistributedLogsConversionResource(pipeline);
  }
}
