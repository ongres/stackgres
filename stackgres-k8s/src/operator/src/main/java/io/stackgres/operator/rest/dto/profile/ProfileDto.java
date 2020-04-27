/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.profile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.ResourceDto;

@RegisterForReflection
public class ProfileDto extends ResourceDto {

  @NotNull(message = "The specification of profile is required")
  @Valid
  private ProfileSpec spec;

  public ProfileSpec getSpec() {
    return spec;
  }

  public void setSpec(ProfileSpec spec) {
    this.spec = spec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

}
