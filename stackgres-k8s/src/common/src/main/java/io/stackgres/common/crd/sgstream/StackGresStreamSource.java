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
      message = "type must be one of SGCluster or Postgres")
  private String type;

  @Valid
  private StackGresStreamSourceSgCluster sgCluster;

  @Valid
  private StackGresStreamSourcePostgres postgres;

  @ReferencedField("type")
  interface Type extends FieldReference {
  }

  @ReferencedField("sgCluster")
  interface SgCluster extends FieldReference {
  }

  @ReferencedField("postgres")
  interface Postgres extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "type must match corresponding section.",
      payload = Type.class)
  public boolean isTypeMatchSection() {
    if (type != null) {
      switch (type) {
        case "SGCluster":
          return postgres == null;
        case "Postgres":
          return sgCluster == null;
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

  @JsonIgnore
  @AssertTrue(message = "postgres must not be null",
      payload = Postgres.class)
  public boolean isPostgresPresent() {
    return !Objects.equals(type, "Postgres") || postgres != null;
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

  public StackGresStreamSourcePostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(StackGresStreamSourcePostgres postgres) {
    this.postgres = postgres;
  }

  @Override
  public int hashCode() {
    return Objects.hash(postgres, sgCluster, type);
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
    return Objects.equals(postgres, other.postgres) && Objects.equals(sgCluster, other.sgCluster)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
