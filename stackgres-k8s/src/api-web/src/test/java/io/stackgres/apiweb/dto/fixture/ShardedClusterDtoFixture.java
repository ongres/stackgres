/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.common.StackGresComponent;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedClusterDtoFixture extends Fixture<ShardedClusterDto> {

  public static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().get(0).get();

  public ShardedClusterDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_DTO_JSON);
    return this;
  }

  public ShardedClusterDtoFixture loadInlineScripts() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_INLINE_SCRIPTS_JSON);
    return this;
  }

  public ShardedClusterDtoFixture empty() {
    fixture = new ShardedClusterDto();
    return this;
  }

  public ShardedClusterDtoFixture withLatestPostgresVersion() {
    fixture.getSpec().getPostgres().setVersion(POSTGRES_LATEST_VERSION);
    return this;
  }

}
