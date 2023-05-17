/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.postgres.service.PostgresService;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomServicePort;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS",
    justification = "equals and hashCode are unused")
public class ClusterPostgresService extends PostgresService {

  private static final long serialVersionUID = 1L;

  private List<CustomServicePort> customPorts;

  public List<CustomServicePort> getCustomPorts() {
    return customPorts;
  }

  public void setCustomPorts(List<CustomServicePort> customPorts) {
    this.customPorts = customPorts;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
