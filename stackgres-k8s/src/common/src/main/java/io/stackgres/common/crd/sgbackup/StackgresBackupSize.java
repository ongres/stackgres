/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackgresBackupSize {

  private Long uncompressed;
  private Long compressed;

  public Long getUncompressed() {
    return uncompressed;
  }

  public void setUncompressed(Long uncompressed) {
    this.uncompressed = uncompressed;
  }

  public Long getCompressed() {
    return compressed;
  }

  public void setCompressed(Long compressed) {
    this.compressed = compressed;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uncompressed", uncompressed)
        .add("compressed", compressed)
        .toString();
  }
}
