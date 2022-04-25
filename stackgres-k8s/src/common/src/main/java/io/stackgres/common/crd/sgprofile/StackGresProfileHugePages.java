/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
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
