/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamSpecMetadata {

  private StreamSpecAnnotations annotations;

  private StreamSpecLabels labels;

  public StreamSpecAnnotations getAnnotations() {
    return annotations;
  }

  public StreamSpecLabels getLabels() {
    return labels;
  }

  public void setLabels(StreamSpecLabels labels) {
    this.labels = labels;
  }

  public void setAnnotations(StreamSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
