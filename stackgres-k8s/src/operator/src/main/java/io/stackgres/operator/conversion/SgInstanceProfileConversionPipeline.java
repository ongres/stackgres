/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgprofile.StackGresProfileDefinition;

@ApplicationScoped
@Conversion(StackGresProfileDefinition.KIND)
public class SgInstanceProfileConversionPipeline implements ConversionPipeline {

}
