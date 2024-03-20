/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.List;
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
public class NamespaceSelector {

  private Boolean any;
  private List<String> matchNames;

  public Boolean getAny() {
    return any;
  }

  public void setAny(Boolean any) {
    this.any = any;
  }

  public List<String> getMatchNames() {
    return matchNames;
  }

  public void setMatchNames(List<String> matchNames) {
    this.matchNames = matchNames;
  }

  @Override
  public int hashCode() {
    return Objects.hash(any, matchNames);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof NamespaceSelector)) {
      return false;
    }
    NamespaceSelector other = (NamespaceSelector) obj;
    return Objects.equals(any, other.any) && Objects.equals(matchNames, other.matchNames);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
