/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.Map;
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
public class PrometheusInstallation {

  private String namespace;

  private Map<String, String> matchLabels;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Map<String, String> getMatchLabels() {
    return matchLabels;
  }

  public void setMatchLabels(Map<String, String> matchLabels) {
    this.matchLabels = matchLabels;
  }

  @Override
  public int hashCode() {
    return Objects.hash(matchLabels, namespace);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PrometheusInstallation)) {
      return false;
    }
    PrometheusInstallation other = (PrometheusInstallation) obj;
    return Objects.equals(matchLabels, other.matchLabels)
        && Objects.equals(namespace, other.namespace);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

