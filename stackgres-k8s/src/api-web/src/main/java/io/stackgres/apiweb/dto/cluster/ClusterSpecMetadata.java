/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterSpecMetadata {

  private ClusterSpecAnnotations annotations;

  public ClusterSpecAnnotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(ClusterSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("annotations", annotations)
        .toString();
  }
}
