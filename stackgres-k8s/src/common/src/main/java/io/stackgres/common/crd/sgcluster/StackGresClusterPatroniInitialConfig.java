/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.JsonObject;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresClusterPatroniInitialConfig extends JsonObject {

  private static final long serialVersionUID = 1L;

  public StackGresClusterPatroniInitialConfig() {
    super();
  }

  public StackGresClusterPatroniInitialConfig(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public StackGresClusterPatroniInitialConfig(int initialCapacity) {
    super(initialCapacity);
  }

  public StackGresClusterPatroniInitialConfig(Map<? extends String, ? extends Object> m) {
    super(m);
  }

  public String getScope() {
    return (String) get("scope");
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
