/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgDistributedLogsConversionResourceTest
    extends ConversionResourceTest<StackGresDistributedLogs> {

  @Override
  protected StackGresDistributedLogs getCustomResource() {
    return JsonUtil.readFromJson("distributedlogs/default.json",
        StackGresDistributedLogs.class);
  }

  @Override
  protected ConversionResource getConversionResource() {
    return new SgDistributedLogsConversionResource(pipeline);
  }
}