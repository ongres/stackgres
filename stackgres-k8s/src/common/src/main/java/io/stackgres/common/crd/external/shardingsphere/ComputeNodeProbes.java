/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.shardingsphere;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Probe;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ComputeNodeProbes {

  private Probe livenessProbe;

  private Probe readinessProbe;

  private Probe startupProbe;

  public Probe getLivenessProbe() {
    return livenessProbe;
  }

  public void setLivenessProbe(Probe livenessProbe) {
    this.livenessProbe = livenessProbe;
  }

  public Probe getReadinessProbe() {
    return readinessProbe;
  }

  public void setReadinessProbe(Probe readinessProbe) {
    this.readinessProbe = readinessProbe;
  }

  public Probe getStartupProbe() {
    return startupProbe;
  }

  public void setStartupProbe(Probe startupProbe) {
    this.startupProbe = startupProbe;
  }

  @Override
  public int hashCode() {
    return Objects.hash(livenessProbe, readinessProbe, startupProbe);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComputeNodeProbes)) {
      return false;
    }
    ComputeNodeProbes other = (ComputeNodeProbes) obj;
    return Objects.equals(livenessProbe, other.livenessProbe) && Objects.equals(readinessProbe, other.readinessProbe)
        && Objects.equals(startupProbe, other.startupProbe);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
