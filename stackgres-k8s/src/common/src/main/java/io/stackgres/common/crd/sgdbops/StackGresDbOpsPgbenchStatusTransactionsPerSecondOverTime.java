/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime {

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
  public int hashCode() {
    return Objects.hash(intervalDuration, intervalDurationUnit, values, valuesUnit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime)) {
      return false;
    }
    StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime other =
        (StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime) obj;
    return Objects.equals(intervalDuration, other.intervalDuration)
        && Objects.equals(intervalDurationUnit, other.intervalDurationUnit)
        && Objects.equals(values, other.values) && Objects.equals(valuesUnit, other.valuesUnit);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
