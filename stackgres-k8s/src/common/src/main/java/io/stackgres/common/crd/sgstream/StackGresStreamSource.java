/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamSource {

  @ValidEnum(enumClass = StreamSourceType.class, allowNulls = false,
      message = "type must be SGCluster")
  private String type;

  @Valid
  private StackGresStreamSourceSgCluster sgCluster;

  @ReferencedField("type")
  interface Type extends FieldReference {
  }

  @ReferencedField("sgCluster")
  interface SgCluster extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "type must match corresponding section.",
      payload = Type.class)
  public boolean isTypeMatchSection() {
    if (type != null) {
      switch (type) {
        case "SGCluster":
          return true;
        default:
          break;
      }
    }
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "sgCluster must not be null",
      payload = SgCluster.class)
  public boolean isSgClusterPresent() {
    return !Objects.equals(type, "SGCluster") || sgCluster != null;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public StackGresStreamSourceSgCluster getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(StackGresStreamSourceSgCluster sgCluster) {
    this.sgCluster = sgCluster;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sgCluster, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamSource)) {
      return false;
    }
    StackGresStreamSource other = (StackGresStreamSource) obj;
    return Objects.equals(sgCluster, other.sgCluster) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
