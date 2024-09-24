/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class SecretOrConfigMap {

  private SecretKeySelector secret;

  private ConfigMapKeySelector configMap;

  public SecretKeySelector getSecret() {
    return secret;
  }

  public void setSecret(SecretKeySelector secret) {
    this.secret = secret;
  }

  public ConfigMapKeySelector getConfigMap() {
    return configMap;
  }

  public void setConfigMap(ConfigMapKeySelector configMap) {
    this.configMap = configMap;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configMap, secret);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SecretOrConfigMap)) {
      return false;
    }
    SecretOrConfigMap other = (SecretOrConfigMap) obj;
    return Objects.equals(configMap, other.configMap) && Objects.equals(secret, other.secret);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
