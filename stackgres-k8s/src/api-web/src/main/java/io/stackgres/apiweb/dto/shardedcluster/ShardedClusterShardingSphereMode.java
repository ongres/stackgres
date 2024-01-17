/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShardedClusterShardingSphereMode {

  private String type;

  private ShardedClusterShardingSphereRepository repository;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ShardedClusterShardingSphereRepository getRepository() {
    return repository;
  }

  public void setRepository(ShardedClusterShardingSphereRepository repository) {
    this.repository = repository;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
