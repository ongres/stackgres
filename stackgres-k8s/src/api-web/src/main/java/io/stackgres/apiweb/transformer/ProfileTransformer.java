/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.dto.profile.ProfileSpec;
import io.stackgres.apiweb.dto.profile.ProfileStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;

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
        .map(o -> mapper.convertValue(original, StackGresProfile.class))
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
    if (transformation.getStatus() == null) {
      transformation.setStatus(new ProfileStatus());
    }
    transformation.getStatus().setClusters(clusters);
    return transformation;
  }

  private StackGresProfileSpec getCustomResourceSpec(ProfileSpec source) {
    return mapper.convertValue(source, StackGresProfileSpec.class);
  }

  private ProfileSpec getResourceSpec(StackGresProfileSpec source) {
    return mapper.convertValue(source, ProfileSpec.class);
  }

}
