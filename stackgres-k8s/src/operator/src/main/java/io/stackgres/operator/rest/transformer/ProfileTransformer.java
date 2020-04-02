/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.rest.dto.profile.ProfileDto;
import io.stackgres.operator.rest.dto.profile.ProfileSpec;

@ApplicationScoped
public class ProfileTransformer
    extends AbstractResourceTransformer<ProfileDto, StackGresProfile> {

  @Override
  public StackGresProfile toCustomResource(ProfileDto source, StackGresProfile original) {
    StackGresProfile transformation = Optional.ofNullable(original)
        .orElseGet(StackGresProfile::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ProfileDto toResource(StackGresProfile source) {
    ProfileDto transformation = new ProfileDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  private StackGresProfileSpec getCustomResourceSpec(ProfileSpec source) {
    if (source == null) {
      return null;
    }
    StackGresProfileSpec transformation = new StackGresProfileSpec();
    transformation.setCpu(source.getCpu());
    transformation.setMemory(source.getMemory());
    return transformation;
  }

  private ProfileSpec getResourceSpec(StackGresProfileSpec source) {
    if (source == null) {
      return null;
    }
    ProfileSpec transformation = new ProfileSpec();
    transformation.setCpu(source.getCpu());
    transformation.setMemory(source.getMemory());
    return transformation;
  }

}
