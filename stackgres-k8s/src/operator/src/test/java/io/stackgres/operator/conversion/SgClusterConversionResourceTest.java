/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgClusterConversionResourceTest extends ConversionResourceTest<StackGresCluster> {

  @Override
  protected StackGresCluster getCustomResource() {
    return Fixtures.cluster().loadDefault().get();
  }

  @Override
  protected ConversionResource getConversionResource() {
    return new SgClusterConversionResource(pipeline);
  }

}
