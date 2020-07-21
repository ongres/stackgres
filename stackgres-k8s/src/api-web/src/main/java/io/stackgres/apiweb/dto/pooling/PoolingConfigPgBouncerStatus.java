/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pooling;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PoolingConfigPgBouncerStatus {

  @JsonProperty("pgbouncer.ini")
  @NotNull(message = "pgbouncer.ini is required")
  @Valid
  private List<PgBouncerIniParameter> pgbouncerConf;

  @JsonProperty("defaultParameters")
  @NotNull(message = "defaultParameters is required")
  private List<String> defaultParameters;

  public List<PgBouncerIniParameter> getPgbouncerConf() {
    return pgbouncerConf;
  }

  public void setPgbouncerConf(List<PgBouncerIniParameter> pgbouncerConf) {
    this.pgbouncerConf = pgbouncerConf;
  }

  public List<String> getDefaultParameters() {
    return defaultParameters;
  }

  public void setDefaultParameters(List<String> defaultParameters) {
    this.defaultParameters = defaultParameters;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pgbouncerConf", pgbouncerConf)
        .add("defaultParameters", defaultParameters)
        .toString();
  }
}
