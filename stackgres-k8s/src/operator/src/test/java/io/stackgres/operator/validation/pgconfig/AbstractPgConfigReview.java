/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.testutil.JsonUtil;

abstract class AbstractPgConfigReview {

  protected PgConfigReview validConfigReview() {
    return JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);
  }

  protected PgConfigReview validConfigUpdate() {
    return JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);
  }

  protected PgConfigReview validConfigDelete() {
    return JsonUtil.readFromJson("pgconfig_allow_request/pgconfig_delete.json",
        PgConfigReview.class);
  }

}
