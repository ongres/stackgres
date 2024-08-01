/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTargetPgLambdaKnative {

  private Map<String, String> annotations;

  private Map<String, String> labels;

  @Valid
  private StackGresStreamTargetCloudEventHttp http;

  public Map<String, String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public StackGresStreamTargetCloudEventHttp getHttp() {
    return http;
  }

  public void setHttp(StackGresStreamTargetCloudEventHttp http) {
    this.http = http;
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations, http, labels);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamTargetPgLambdaKnative)) {
      return false;
    }
    StackGresStreamTargetPgLambdaKnative other = (StackGresStreamTargetPgLambdaKnative) obj;
    return Objects.equals(annotations, other.annotations) && Objects.equals(http, other.http)
        && Objects.equals(labels, other.labels);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
