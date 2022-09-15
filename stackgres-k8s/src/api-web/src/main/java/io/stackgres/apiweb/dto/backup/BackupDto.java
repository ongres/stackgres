/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BackupDto extends ResourceDto {

  private BackupSpec spec;

  private BackupStatus status;

  public BackupSpec getSpec() {
    return spec;
  }

  public void setSpec(BackupSpec spec) {
    this.spec = spec;
  }

  public BackupStatus getStatus() {
    return status;
  }

  public void setStatus(BackupStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
