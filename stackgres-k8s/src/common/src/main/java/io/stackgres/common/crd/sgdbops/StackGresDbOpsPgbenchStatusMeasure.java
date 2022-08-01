/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsPgbenchStatusMeasure implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("value")
  private BigDecimal value;

  @JsonProperty("unit")
  private String unit;

  public BigDecimal getValue() {
    return value;
  }

  public StackGresDbOpsPgbenchStatusMeasure() { }

  public StackGresDbOpsPgbenchStatusMeasure(BigDecimal value, String unit) {
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
