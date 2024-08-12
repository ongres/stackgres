/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchStatusMeasure {

  private BigDecimal value;

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
