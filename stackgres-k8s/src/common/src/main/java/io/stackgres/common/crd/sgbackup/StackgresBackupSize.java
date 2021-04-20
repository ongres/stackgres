/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

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
    return StackGresUtil.toPrettyYaml(this);
  }
}
