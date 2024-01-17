/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.shardingsphere;

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
public class ComputeNodeTracing {

  private ComputeNodeProps openTelemetry;

  private ComputeNodeProps openTracing;

  public ComputeNodeProps getOpenTelemetry() {
    return openTelemetry;
  }

  public void setOpenTelemetry(ComputeNodeProps openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  public ComputeNodeProps getOpenTracing() {
    return openTracing;
  }

  public void setOpenTracing(ComputeNodeProps openTracing) {
    this.openTracing = openTracing;
  }

  @Override
  public int hashCode() {
    return Objects.hash(openTelemetry, openTracing);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComputeNodeTracing)) {
      return false;
    }
    ComputeNodeTracing other = (ComputeNodeTracing) obj;
    return Objects.equals(openTelemetry, other.openTelemetry) && Objects.equals(openTracing, other.openTracing);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
