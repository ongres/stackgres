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
public class StreamSpec {

  private StreamSource source;

  private StreamTarget target;

  private Integer maxRetries;

  private StreamSpecMetadata metadata;

  private StreamPods pods;

  private StreamDebeziumEngineProperties debeziumEngineProperties;

  public StreamSource getSource() {
    return source;
  }

  public void setSource(StreamSource source) {
    this.source = source;
  }

  public StreamTarget getTarget() {
    return target;
  }

  public void setTarget(StreamTarget target) {
    this.target = target;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public StreamSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(StreamSpecMetadata metadata) {
    this.metadata = metadata;
  }

  public StreamPods getPods() {
    return pods;
  }

  public void setPods(StreamPods pods) {
    this.pods = pods;
  }

  public StreamDebeziumEngineProperties getDebeziumEngineProperties() {
    return debeziumEngineProperties;
  }

  public void setDebeziumEngineProperties(
      StreamDebeziumEngineProperties debeziumEngineProperties) {
    this.debeziumEngineProperties = debeziumEngineProperties;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
