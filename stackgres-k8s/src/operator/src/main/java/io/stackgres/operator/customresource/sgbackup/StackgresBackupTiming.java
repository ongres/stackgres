/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackgresBackupTiming {

  private String start;
  private String end;
  private String stored;

  public String getStored() {
    return stored;
  }

  public void setStored(String stored) {
    this.stored = stored;
  }

  public String getStart() {
    return start;
  }

  public void setStart(String start) {
    this.start = start;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("startTime", start)
        .add("finishTime", end)
        .add("stored", stored)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackgresBackupTiming that = (StackgresBackupTiming) o;
    return Objects.equals(start, that.start) && Objects.equals(end, that.end)
        && Objects.equals(stored, that.stored);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, stored);
  }
}
