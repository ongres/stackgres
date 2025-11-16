/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterSpecMetadata {

  private ShardedClusterSpecAnnotations annotations;

  private ShardedClusterSpecLabels labels;

  public ShardedClusterSpecAnnotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(ShardedClusterSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  public ShardedClusterSpecLabels getLabels() {
    return labels;
  }

  public void setLabels(ShardedClusterSpecLabels labels) {
    this.labels = labels;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
