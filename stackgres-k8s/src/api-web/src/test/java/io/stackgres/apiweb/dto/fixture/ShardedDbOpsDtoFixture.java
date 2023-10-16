/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedDbOpsDtoFixture extends Fixture<ShardedDbOpsDto> {

  public ShardedDbOpsDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_DTO_JSON);
    return this;
  }

}
