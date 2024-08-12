/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ScriptFrom {

  private String secretScript;

  private SecretKeySelector secretKeyRef;

  private String configMapScript;

  private ConfigMapKeySelector configMapKeyRef;

  public String getSecretScript() {
    return secretScript;
  }

  public void setSecretScript(String secretScript) {
    this.secretScript = secretScript;
  }

  public SecretKeySelector getSecretKeyRef() {
    return secretKeyRef;
  }

  public void setSecretKeyRef(SecretKeySelector secretKeyRef) {
    this.secretKeyRef = secretKeyRef;
  }

  public String getConfigMapScript() {
    return configMapScript;
  }

  public void setConfigMapScript(String configMapScript) {
    this.configMapScript = configMapScript;
  }

  public ConfigMapKeySelector getConfigMapKeyRef() {
    return configMapKeyRef;
  }

  public void setConfigMapKeyRef(ConfigMapKeySelector configMapKeyRef) {
    this.configMapKeyRef = configMapKeyRef;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
