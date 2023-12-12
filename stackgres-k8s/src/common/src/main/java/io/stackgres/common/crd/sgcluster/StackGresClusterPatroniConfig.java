/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.JsonObject;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresClusterPatroniConfig extends JsonObject {

  private static final long serialVersionUID = 1L;

  public StackGresClusterPatroniConfig() {
    super();
  }

  public StackGresClusterPatroniConfig(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public StackGresClusterPatroniConfig(int initialCapacity) {
    super(initialCapacity);
  }

  public StackGresClusterPatroniConfig(Map<? extends String, ? extends Object> m) {
    super(m);
  }

  public String getScope() {
    return (String) get("scope");
  }

  public Optional<Integer> getCitusGroup() {
    return Optional.of(this)
        .filter(config -> config.hasObject("citus"))
        .map(config -> config.getObject("citus"))
        .map(config -> config.get("group"))
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
