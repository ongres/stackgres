/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto.pooling;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PoolingConfigSpec {

  private PolingConfigPgBouncer pgBouncer;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("pgBouncer", getPgBouncer())
        .toString();
  }

  public PolingConfigPgBouncer getPgBouncer() {
    return pgBouncer;
  }

  public void setPgBouncer(PolingConfigPgBouncer pgBouncer) {
    this.pgBouncer = pgBouncer;
  }
}
