/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.JsonObject;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresClusterPatroniDynamicConfig extends JsonObject {

  public StackGresClusterPatroniDynamicConfig() {
    super();
  }

  public StackGresClusterPatroniDynamicConfig(Map<String, Object> m) {
    super(m);
  }

  @JsonIgnore
  public Optional<JsonObject> getPostgresql() {
    return Optional.of(this)
        .filter(config -> config.hasObject("postgresql"))
        .map(config -> config.getObject("postgresql"));
  }

  @JsonIgnore
  public Optional<List<String>> getPgHba() {
    return getPostgresql()
        .map(postgresql -> postgresql.get("pg_hba"))
        .filter(List.class::isInstance)
        .map(List.class::cast);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
