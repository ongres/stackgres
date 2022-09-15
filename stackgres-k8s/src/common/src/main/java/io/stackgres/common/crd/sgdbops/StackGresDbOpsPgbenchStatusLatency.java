/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsPgbenchStatusLatency {

  @JsonProperty("average")
  private StackGresDbOpsPgbenchStatusMeasure average;

  @JsonProperty("standardDeviation")
  private StackGresDbOpsPgbenchStatusMeasure standardDeviation;

  public StackGresDbOpsPgbenchStatusMeasure getAverage() {
    return average;
  }

  public void setAverage(StackGresDbOpsPgbenchStatusMeasure average) {
    this.average = average;
  }

  public StackGresDbOpsPgbenchStatusMeasure getStandardDeviation() {
    return standardDeviation;
  }

  public void setStandardDeviation(StackGresDbOpsPgbenchStatusMeasure standardDeviation) {
    this.standardDeviation = standardDeviation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(average, standardDeviation);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatusLatency)) {
      return false;
    }
    StackGresDbOpsPgbenchStatusLatency other = (StackGresDbOpsPgbenchStatusLatency) obj;
    return Objects.equals(average, other.average)
        && Objects.equals(standardDeviation, other.standardDeviation);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
