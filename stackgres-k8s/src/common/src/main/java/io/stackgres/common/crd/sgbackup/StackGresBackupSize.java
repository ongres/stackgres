/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupSize {

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
  public int hashCode() {
    return Objects.hash(compressed, uncompressed);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupSize)) {
      return false;
    }
    StackGresBackupSize other = (StackGresBackupSize) obj;
    return Objects.equals(compressed, other.compressed)
        && Objects.equals(uncompressed, other.uncompressed);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
