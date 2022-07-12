/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresPoolingConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 2000013861182789247L;

  @NotNull(message = "pgBouncer configuration should not be empty")
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
