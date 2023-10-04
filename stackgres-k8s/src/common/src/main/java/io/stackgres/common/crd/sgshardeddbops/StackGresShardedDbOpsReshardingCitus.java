/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import java.math.BigDecimal;
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
public class StackGresShardedDbOpsReshardingCitus {

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
  public int hashCode() {
    return Objects.hash(drainOnly, rebalanceStrategy, threshold);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedDbOpsReshardingCitus)) {
      return false;
    }
    StackGresShardedDbOpsReshardingCitus other = (StackGresShardedDbOpsReshardingCitus) obj;
    return Objects.equals(drainOnly, other.drainOnly)
        && Objects.equals(rebalanceStrategy, other.rebalanceStrategy)
        && Objects.equals(threshold, other.threshold);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
