/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedDbOpsReshardingCitus {

  protected BigDecimal threshold;

  protected Boolean drainOnly;

  protected String rebalanceStrategy;

  public BigDecimal getThreshold() {
    return threshold;
  }

  public void setThreshold(BigDecimal threshold) {
    this.threshold = threshold;
  }

  public Boolean getDrainOnly() {
    return drainOnly;
  }

  public void setDrainOnly(Boolean drainOnly) {
    this.drainOnly = drainOnly;
  }

  public String getRebalanceStrategy() {
    return rebalanceStrategy;
  }

  public void setRebalanceStrategy(String rebalanceStrategy) {
    this.rebalanceStrategy = rebalanceStrategy;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
