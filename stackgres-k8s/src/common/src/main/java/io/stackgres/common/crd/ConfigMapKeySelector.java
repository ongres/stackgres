/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"optional"})
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ConfigMapKeySelector.class)
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class ConfigMapKeySelector extends io.fabric8.kubernetes.api.model.ConfigMapKeySelector {

  private static final long serialVersionUID = 1L;

  public ConfigMapKeySelector() {
    super();
  }

  public ConfigMapKeySelector(String key, String name) {
    super(key, name, false);
  }

  @ReferencedField("key")
  interface Key extends FieldReference { }

  @ReferencedField("name")
  interface Name extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "key must not be empty.",
      payload = Key.class)
  public boolean isKeyNotNull() {
    return getKey() != null && !getKey().isEmpty();
  }

  @JsonIgnore
  @AssertTrue(message = "name must not be empty.",
      payload = Name.class)
  public boolean isNameNotNull() {
    return getName() != null && !getName().isEmpty();
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
  public String getName() {
    return super.getName();
  }

  @Override
  public void setName(String name) {
    super.setName(name);
  }

  @Override
  public Boolean getOptional() {
    return super.getOptional();
  }

  @Override
  public void setOptional(Boolean optional) {
    super.setOptional(optional);
  }

}
