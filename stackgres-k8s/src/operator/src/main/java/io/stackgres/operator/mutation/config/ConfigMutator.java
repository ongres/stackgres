/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.config;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.StackGresConfigReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface ConfigMutator extends Mutator<StackGresConfig, StackGresConfigReview> {

}
