/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.common.RestoreConfigReview;

public interface WithRestoreReviewResources {

  default RestoreConfigReview getCreationReview() {
    return JsonUtil.readFromJson("restore_config_allow_request/create.json",
        RestoreConfigReview.class);
  }

  default RestoreConfigReview getUpdateReview() {
    return JsonUtil.readFromJson("restore_config_allow_request/update.json",
        RestoreConfigReview.class);
  }
}
