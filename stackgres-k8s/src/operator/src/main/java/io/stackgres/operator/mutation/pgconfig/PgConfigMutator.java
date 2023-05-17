/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface PgConfigMutator extends Mutator<StackGresPostgresConfig, PgConfigReview> {

}
