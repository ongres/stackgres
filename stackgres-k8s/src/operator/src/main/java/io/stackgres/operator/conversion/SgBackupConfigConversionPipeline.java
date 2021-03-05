/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;

@ApplicationScoped
@Conversion(StackGresBackupConfig.KIND)
public class SgBackupConfigConversionPipeline implements ConversionPipeline {

}
