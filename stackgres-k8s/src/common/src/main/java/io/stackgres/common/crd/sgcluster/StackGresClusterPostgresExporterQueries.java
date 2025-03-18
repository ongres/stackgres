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
public class StackGresClusterPostgresExporterQueries extends JsonObject {

  public StackGresClusterPostgresExporterQueries() {
    super();
  }

  public StackGresClusterPostgresExporterQueries(Map<String, Object> m) {
    super(m);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
