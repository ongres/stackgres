/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public class ProfileDto extends ResourceDto {

  @NotNull(message = "The specification of profile is required")
  @Valid
  private ProfileSpec spec;

  @Valid
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
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ProfileDto)) {
      return false;
    }
    ProfileDto other = (ProfileDto) obj;
    return Objects.equals(spec, other.spec) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
