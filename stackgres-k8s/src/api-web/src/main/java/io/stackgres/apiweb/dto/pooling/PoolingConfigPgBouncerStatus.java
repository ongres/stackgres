/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pooling;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PoolingConfigPgBouncerStatus {

  @JsonProperty("pgbouncer.ini")
  private List<PgBouncerIniParameter> pgbouncerIni;

  private Map<String, String> defaultParameters;

  public List<PgBouncerIniParameter> getPgbouncerIni() {
    return pgbouncerIni;
  }

  public void setPgbouncerIni(List<PgBouncerIniParameter> pgbouncerIni) {
    this.pgbouncerIni = pgbouncerIni;
  }

  public Map<String, String> getDefaultParameters() {
    return defaultParameters;
  }

  public void setDefaultParameters(Map<String, String> defaultParameters) {
    this.defaultParameters = defaultParameters;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
