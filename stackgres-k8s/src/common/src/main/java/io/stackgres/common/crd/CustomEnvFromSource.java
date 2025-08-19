/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.EnvFromSource.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ConfigMapEnvSource.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.SecretEnvSource.class),
    })
public class CustomEnvFromSource extends io.fabric8.kubernetes.api.model.EnvFromSource {

  private static final long serialVersionUID = 1L;

  public CustomEnvFromSource() {
    super();
  }

  public CustomEnvFromSource(
      ConfigMapEnvSource configMapRef,
      String prefix,
      SecretEnvSource secretRef) {
    super(
        configMapRef,
        prefix,
        secretRef);
  }

  public ConfigMapEnvSource getConfigMapRef() {
    return super.getConfigMapRef();
  }

  public void setConfigMapRef(ConfigMapEnvSource configMapRef) {
    super.setConfigMapRef(configMapRef);
  }

  public String getPrefix() {
    return super.getPrefix();
  }

  public void setPrefix(String prefix) {
    super.setPrefix(prefix);
  }

  public SecretEnvSource getSecretRef() {
    return super.getSecretRef();
  }

  public void setSecretRef(SecretEnvSource secretRef) {
    super.setSecretRef(secretRef);
  }

}
