/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.dto.profile.ProfileSpec;
import io.stackgres.apiweb.dto.profile.ProfileStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProfileTransformer
    extends AbstractDependencyResourceTransformer<ProfileDto, StackGresProfile> {

  private final ObjectMapper mapper;

  @Inject
  public ProfileTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresProfile toCustomResource(ProfileDto source, StackGresProfile original) {
    StackGresProfile transformation = Optional.ofNullable(original)
        .map(crd -> mapper.convertValue(crd, StackGresProfile.class))
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
    return mapper.convertValue(source, StackGresProfileSpec.class);
  }

  private ProfileSpec getResourceSpec(StackGresProfileSpec source) {
    return mapper.convertValue(source, ProfileSpec.class);
  }

  private ProfileStatus getResourceStatus(List<String> clusters) {
    ProfileStatus transformation = new ProfileStatus();
    transformation.setClusters(clusters);
    return transformation;
  }

}
