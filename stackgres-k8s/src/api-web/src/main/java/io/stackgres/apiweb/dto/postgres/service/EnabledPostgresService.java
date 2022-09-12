/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.postgres.service;

import static java.lang.Boolean.TRUE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties({ "enabled" })
public class EnabledPostgresService extends PostgresService {

  public EnabledPostgresService() {
    super.setEnabled(TRUE);
  }

  @Override
  public Boolean getEnabled() {
    return TRUE;
  }

  @Override
  public void setEnabled(Boolean enabled) {
    throw new UnsupportedOperationException("The operation is not supported. "
        + "EnabledPostgresService.setEnabled is always enabled by default!");
  }
}
