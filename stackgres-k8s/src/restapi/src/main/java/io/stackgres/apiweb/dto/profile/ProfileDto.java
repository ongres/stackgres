/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgprofile.StackGresProfile;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresProfile.class)
public class ProfileDto extends ResourceDto {

  private ProfileSpec spec;

  private ProfileStatus status;

  public ProfileSpec getSpec() {
    return spec;
  }

  public void setSpec(ProfileSpec spec) {
    this.spec = spec;
  }

  public ProfileStatus getStatus() {
    return status;
  }

  public void setStatus(ProfileStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
