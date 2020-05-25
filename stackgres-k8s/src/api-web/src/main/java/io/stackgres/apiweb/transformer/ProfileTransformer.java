/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.apiweb.distributedlogs.dto.profile.ProfileDto;
import io.stackgres.apiweb.distributedlogs.dto.profile.ProfileSpec;
import io.stackgres.apiweb.distributedlogs.dto.profile.ProfileStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;

@ApplicationScoped
public class ProfileTransformer
    extends AbstractDependencyResourceTransformer<ProfileDto, StackGresProfile> {

  @Override
  public StackGresProfile toCustomResource(ProfileDto source, StackGresProfile original) {
    StackGresProfile transformation = Optional.ofNullable(original)
        .orElseGet(StackGresProfile::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ProfileDto toResource(StackGresProfile source, List<String> clusters) {
    ProfileDto transformation = new ProfileDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters));
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

  private ProfileStatus getResourceStatus(List<String> clusters) {
    ProfileStatus transformation = new ProfileStatus();
    transformation.setClusters(clusters);
    return transformation;
  }

}
