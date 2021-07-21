/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DbOpsPgbenchStatusMeasure {

  @JsonProperty("value")
  private BigDecimal value;

  @JsonProperty("unit")
  private String unit;

  public BigDecimal getValue() {
    return value;
  }

  public DbOpsPgbenchStatusMeasure() { }

  public DbOpsPgbenchStatusMeasure(BigDecimal value, String unit) {
    this.value = value;
    this.unit = unit;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

}
