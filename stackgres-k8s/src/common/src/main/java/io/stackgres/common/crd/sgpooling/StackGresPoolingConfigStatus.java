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
public class StackGresPoolingConfigStatus implements KubernetesResource {

  private static final long serialVersionUID = 2000013861182789247L;

  @NotNull(message = "pgBouncer is required")
  @Valid
  private StackGresPoolingConfigPgBouncerStatus pgBouncer;

  public StackGresPoolingConfigPgBouncerStatus getPgBouncer() {
    return pgBouncer;
  }

  public void setPgBouncer(StackGresPoolingConfigPgBouncerStatus pgBouncer) {
    this.pgBouncer = pgBouncer;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pgBouncer);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfigStatus)) {
      return false;
    }
    StackGresPoolingConfigStatus other = (StackGresPoolingConfigStatus) obj;
    return Objects.equals(pgBouncer, other.pgBouncer);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
