/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPoolingConfigPgBouncer {

  @JsonProperty("pgbouncer.ini")
  @NotEmpty(message = "pgbouncer.ini should not be empty")
  private Map<String, String> pgbouncerConf;

  public Map<String, String> getPgbouncerConf() {
    return pgbouncerConf;
  }

  public void setPgbouncerConf(Map<String, String> pgbouncerConf) {
    this.pgbouncerConf = pgbouncerConf;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresPoolingConfigPgBouncer that = (StackGresPoolingConfigPgBouncer) o;
    return Objects.equals(pgbouncerConf, that.pgbouncerConf);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pgbouncerConf);
  }
}
