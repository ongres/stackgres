/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app.postgres.service;

import static java.lang.Boolean.TRUE;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EnabledPostgresService extends PostgresService {

  private static final long serialVersionUID = 1L;

  public EnabledPostgresService() {}

  public EnabledPostgresService(String type, List<String> externalIPs) {
    super(TRUE, type, externalIPs);
  }

  public EnabledPostgresService(String type) {
    super(TRUE, type);
  }

  @JsonIgnore
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
