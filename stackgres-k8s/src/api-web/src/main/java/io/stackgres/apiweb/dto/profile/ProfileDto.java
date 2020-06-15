/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;

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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("status", status)
        .toString();
  }

}
