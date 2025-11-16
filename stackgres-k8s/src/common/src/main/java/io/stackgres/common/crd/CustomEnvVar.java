/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.EnvVar.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.EnvVarSource.class),
    })
public class CustomEnvVar extends io.fabric8.kubernetes.api.model.EnvVar {

  private static final long serialVersionUID = 1L;

  public CustomEnvVar() {
    super();
  }

  public CustomEnvVar(
      String name,
      String value,
      EnvVarSource valueFrom) {
    super(
        name,
        value,
        valueFrom);
  }

  public String getName() {
    return super.getName();
  }

  public void setName(String name) {
    super.setName(name);
  }

  public String getValue() {
    return super.getValue();
  }

  public void setValue(String value) {
    super.setValue(value);
  }

  public EnvVarSource getValueFrom() {
    return super.getValueFrom();
  }

  public void setValueFrom(EnvVarSource valueFrom) {
    super.setValueFrom(valueFrom);
  }

}
