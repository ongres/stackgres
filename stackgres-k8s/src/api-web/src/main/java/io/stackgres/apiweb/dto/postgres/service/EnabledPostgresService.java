/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.postgres.service;

import static java.lang.Boolean.TRUE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties({ "enabled" })
@SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS",
    justification = "equals and hashCode are unused")
public class EnabledPostgresService extends PostgresService {

  private static final long serialVersionUID = 1L;

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
