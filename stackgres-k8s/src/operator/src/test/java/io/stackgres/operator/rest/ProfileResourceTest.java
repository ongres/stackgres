/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.profile.ProfileDto;
import io.stackgres.operator.rest.transformer.ProfileTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileResourceTest {

  @Mock
  private CustomResourceFinder<StackGresProfile> finder;

  @Mock
  private CustomResourceScanner<StackGresProfile> scanner;

  @Mock
  private CustomResourceScheduler<StackGresProfile> scheduler;

  private StackGresProfileList profiles;

  private ProfileDto profileDto;

  private ProfileResource resource;

  @BeforeEach
  void setUp() {
    profiles = JsonUtil
        .readFromJson("stackgres_profiles/list.json", StackGresProfileList.class);
    profileDto = JsonUtil
        .readFromJson("stackgres_profiles/dto.json", ProfileDto.class);

    resource = new ProfileResource(scanner, finder, scheduler,
        new ProfileTransformer());
  }

  @Test
  void listShouldReturnAllProfiles() {
    when(scanner.getResources()).thenReturn(profiles.getItems());

    List<ProfileDto> profiles = resource.list();

    assertEquals(1, profiles.size());

    assertNotNull(profiles.get(0).getMetadata());

    assertEquals("default", profiles.get(0).getMetadata().getNamespace());

    assertEquals("size-s", profiles.get(0).getMetadata().getName());
  }

  @Test
  void getOfAnExistingProfileShouldReturnTheExistingProfile() {
    when(finder.findByNameAndNamespace("size-s", "default"))
        .thenReturn(Optional.of(profiles.getItems().get(0)));

    ProfileDto profile = resource.get("default", "size-s");

    assertNotNull(profile.getMetadata());

    assertEquals("default", profile.getMetadata().getNamespace());

    assertEquals("size-s", profile.getMetadata().getName());
  }

  @Test
  void createShouldNotFail() {
    resource.create(profileDto);
  }

  @Test
  void updateShouldNotFail() {
    resource.update(profileDto);
  }

  @Test
  void deleteShouldNotFail() {
    resource.delete(profileDto);
  }

}