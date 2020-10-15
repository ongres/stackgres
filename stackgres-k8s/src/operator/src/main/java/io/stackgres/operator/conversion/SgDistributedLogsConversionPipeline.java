/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;

@ApplicationScoped
@Conversion(StackGresDistributedLogsDefinition.KIND)
public class SgDistributedLogsConversionPipeline implements ConversionPipeline {

}
