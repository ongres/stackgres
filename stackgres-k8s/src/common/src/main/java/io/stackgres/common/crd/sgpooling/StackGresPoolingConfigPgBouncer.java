/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresPoolingConfigPgBouncer {

  @JsonProperty("pgbouncer.ini")
  @NotNull(message = "pgbouncer.ini is required")
  @Valid
  private StackGresPoolingConfigPgBouncerPgbouncerIni pgbouncerIni;

  public StackGresPoolingConfigPgBouncerPgbouncerIni getPgbouncerIni() {
    return pgbouncerIni;
  }

  public void setPgbouncerIni(StackGresPoolingConfigPgBouncerPgbouncerIni pgbouncerIni) {
    this.pgbouncerIni = pgbouncerIni;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresPoolingConfigPgBouncer that = (StackGresPoolingConfigPgBouncer) o;
    return Objects.equals(pgbouncerIni, that.pgbouncerIni);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pgbouncerIni);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
