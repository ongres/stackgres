/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.prometheus;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("any", any)
        .add("matchNames", matchNames)
        .toString();
  }
}
