/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.Toleration.class)
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class Toleration extends io.fabric8.kubernetes.api.model.Toleration {

  private static final long serialVersionUID = 1L;

  public Toleration() {
    super();
  }

  public Toleration(String effect, String key, String operator, Long tolerationSeconds,
      String value) {
    super(effect, key, operator, tolerationSeconds, value);
  }

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

  @Override
  public String getEffect() {
    return super.getEffect();
  }

  @Override
  public void setEffect(String effect) {
    super.setEffect(effect);
  }

  @Override
  public String getKey() {
    return super.getKey();
  }

  @Override
  public void setKey(String key) {
    super.setKey(key);
  }

  @Override
  public String getOperator() {
    return super.getOperator();
  }

  @Override
  public void setOperator(String operator) {
    super.setOperator(operator);
  }

  @Override
  public Long getTolerationSeconds() {
    return super.getTolerationSeconds();
  }

  @Override
  public void setTolerationSeconds(Long tolerationSeconds) {
    super.setTolerationSeconds(tolerationSeconds);
  }

  @Override
  public String getValue() {
    return super.getValue();
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);
  }

}
