/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigStatusGrafana {

  private String url;

  private String token;

  private String configHash;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getConfigHash() {
    return configHash;
  }

  public void setConfigHash(String configHash) {
    this.configHash = configHash;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configHash, token, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigStatusGrafana)) {
      return false;
    }
    StackGresConfigStatusGrafana other = (StackGresConfigStatusGrafana) obj;
    return Objects.equals(configHash, other.configHash) && Objects.equals(token, other.token)
        && Objects.equals(url, other.url);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
