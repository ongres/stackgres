/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.math.BigDecimal;
import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresProfileHugePages {

  @JsonProperty("hugepages-2Mi")
  private String hugepages2Mi;

  @JsonProperty("hugepages-1Gi")
  private String hugepages1Gi;

  @ReferencedField("hugepages-2Mi")
  interface Hugepages2Mi extends FieldReference {
  }

  @ReferencedField("hugepages-1Gi")
  interface Hugepages1Gi extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "At least one of hugepages-2Mi or hugepages-1Gi must set",
      payload = {Hugepages2Mi.class, Hugepages1Gi.class})
  public boolean isAnyHugePagesValueSet() {
    return hugepages2Mi != null
        || hugepages1Gi != null;
  }

  @JsonIgnore
  @AssertTrue(message = "hugepages-2Mi must be a multiple of 2Mi",
      payload = {Hugepages2Mi.class})
  public boolean isHugePages2MiValueMultipleOf2Mi() {
    return hugepages2Mi == null
        || (Quantity.getAmountInBytes(new Quantity(hugepages2Mi))
            .remainder(Quantity.getAmountInBytes(new Quantity("2Mi")))
            .compareTo(BigDecimal.ZERO) == 0);
  }

  @JsonIgnore
  @AssertTrue(message = "hugepages-1Gi must be a multiple of 1Gi",
      payload = {Hugepages1Gi.class})
  public boolean isHugePages1GiValueMultipleOf1Gi() {
    return hugepages1Gi == null
        || (Quantity.getAmountInBytes(new Quantity(hugepages1Gi))
            .remainder(Quantity.getAmountInBytes(new Quantity("1Gi")))
            .compareTo(BigDecimal.ZERO) == 0);
  }

  public String getHugepages2Mi() {
    return hugepages2Mi;
  }

  public void setHugepages2Mi(String hugepages2Mi) {
    this.hugepages2Mi = hugepages2Mi;
  }

  public String getHugepages1Gi() {
    return hugepages1Gi;
  }

  public void setHugepages1Gi(String hugepages1Gi) {
    this.hugepages1Gi = hugepages1Gi;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hugepages1Gi, hugepages2Mi);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresProfileHugePages)) {
      return false;
    }
    StackGresProfileHugePages other = (StackGresProfileHugePages) obj;
    return Objects.equals(hugepages1Gi, other.hugepages1Gi)
        && Objects.equals(hugepages2Mi, other.hugepages2Mi);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
