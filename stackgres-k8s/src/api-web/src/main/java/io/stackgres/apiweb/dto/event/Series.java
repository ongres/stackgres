/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public class Series {

  private Integer count;
  private String lastObservedTime;
  private String state;

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getLastObservedTime() {
    return lastObservedTime;
  }

  public void setLastObservedTime(String lastObservedTime) {
    this.lastObservedTime = lastObservedTime;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
