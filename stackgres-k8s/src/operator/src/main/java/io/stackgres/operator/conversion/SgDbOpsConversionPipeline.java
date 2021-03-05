/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;

@ApplicationScoped
@Conversion(StackGresDbOps.KIND)
public class SgDbOpsConversionPipeline implements ConversionPipeline {

}
