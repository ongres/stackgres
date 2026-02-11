/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchStatusTransactionsPerSecondOverTime {

  private List<BigDecimal> values;

  private String valuesUnit;

  private BigDecimal intervalDuration;

  private String intervalDurationUnit;

  public List<BigDecimal> getValues() {
    return values;
  }

  public void setValues(List<BigDecimal> values) {
    this.values = values;
  }

  public String getValuesUnit() {
    return valuesUnit;
  }

  public void setValuesUnit(String valuesUnit) {
    this.valuesUnit = valuesUnit;
  }

  public BigDecimal getIntervalDuration() {
    return intervalDuration;
  }

  public void setIntervalDuration(BigDecimal intervalDuration) {
    this.intervalDuration = intervalDuration;
  }

  public String getIntervalDurationUnit() {
    return intervalDurationUnit;
  }

  public void setIntervalDurationUnit(String intervalDurationUnit) {
    this.intervalDurationUnit = intervalDurationUnit;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
