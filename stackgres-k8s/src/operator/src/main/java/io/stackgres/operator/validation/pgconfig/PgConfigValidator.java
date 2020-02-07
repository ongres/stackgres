/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.cluster.patroni.parameters.Blacklist;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface PgConfigValidator extends Validator<PgConfigReview> {

  String[] BLACKLIST_PROPERTIES = Blacklist.getBlacklistParameters().toArray(new String[0]);


}
