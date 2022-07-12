/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class Toleration extends io.fabric8.kubernetes.api.model.Toleration {

  private static final long serialVersionUID = 1L;

  @ReferencedField("key")
  interface Key extends FieldReference {
  }

  @ReferencedField("operator")
  interface Operator extends FieldReference {
  }

  @ReferencedField("effect")
  interface Effect extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "operator must be Exists when key is empty.",
      payload = {Key.class, Operator.class})
  public boolean isOperatorExistsWhenKeyIsEmpty() {
    return (getKey() != null && !getKey().isEmpty()) // NOPMD
        || Objects.equals(getOperator(), "Exists");
  }

  @JsonIgnore
  @AssertTrue(message = "operator must be Equal or Exists.",
      payload = Operator.class)
  public boolean isOperatorValid() {
    return getOperator() == null
        || ImmutableList.of("Equal", "Exists").contains(getOperator());
  }

  @JsonIgnore
  @AssertTrue(message = "effect must be NoSchedule, PreferNoSchedule or NoExecute.",
      payload = Effect.class)
  public boolean isEffectValid() {
    return getEffect() == null
        || ImmutableList.of("NoSchedule", "PreferNoSchedule", "NoExecute")
        .contains(getEffect());
  }

  @JsonIgnore
  @AssertTrue(message = "effect must be 'NoExecute' when tolerationSeconds is set.",
      payload = Effect.class)
  public boolean isEffectNoExecuteIfTolerationIsSet() {
    return getTolerationSeconds() == null
        || Objects.equals("NoExecute", getEffect());
  }

}
