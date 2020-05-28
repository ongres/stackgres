/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupSize {

  private Long uncompressed;
  private Long compressed;

  public void setCompressed(Long compressed) {
    this.compressed = compressed;
  }

  public Long getCompressed() {
    return compressed;
  }

  public void setUncompressed(Long uncompressed) {
    this.uncompressed = uncompressed;
  }

  public Long getUncompressed() {
    return uncompressed;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uncompressedSize", uncompressed)
        .add("compressedSize", compressed)
        .toString();
  }
}
