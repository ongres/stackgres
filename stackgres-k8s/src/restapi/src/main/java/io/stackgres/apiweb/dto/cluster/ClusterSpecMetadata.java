/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterSpecMetadata {

  private ClusterSpecAnnotations annotations;

  private ClusterSpecLabels labels;

  public ClusterSpecAnnotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(ClusterSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  public ClusterSpecLabels getLabels() {
    return labels;
  }

  public void setLabels(ClusterSpecLabels labels) {
    this.labels = labels;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
