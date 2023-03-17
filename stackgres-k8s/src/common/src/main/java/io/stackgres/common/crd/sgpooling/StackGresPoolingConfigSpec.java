/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresPoolingConfigSpec {

  @NotNull(message = "pgBouncer is required")
  @Valid
  private StackGresPoolingConfigPgBouncer pgBouncer;

  public StackGresPoolingConfigPgBouncer getPgBouncer() {
    return pgBouncer;
  }

  public void setPgBouncer(StackGresPoolingConfigPgBouncer pgBouncer) {
    this.pgBouncer = pgBouncer;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pgBouncer);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresPoolingConfigSpec that = (StackGresPoolingConfigSpec) o;
    return Objects.equals(pgBouncer, that.pgBouncer);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
