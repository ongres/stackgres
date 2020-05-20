/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.ProfileResource;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.apiweb.distributedlogs.dto.profile.ProfileDto;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.ProfileTransformer;
import io.stackgres.testutil.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileResourceTest
    extends AbstractCustomResourceTest<ProfileDto, StackGresProfile, ProfileResource> {

  @Override
  protected CustomResourceList<StackGresProfile> getCustomResourceList() {
    return JsonUtil.readFromJson("stackgres_profiles/list.json", StackGresProfileList.class);
  }

  @Override
  protected ProfileDto getResourceDto() {
    return JsonUtil.readFromJson("stackgres_profiles/dto.json", ProfileDto.class);
  }

  @Override
  protected AbstractResourceTransformer<ProfileDto, StackGresProfile> getTransformer() {
    return new ProfileTransformer();
  }

  @Override
  protected ProfileResource getService(
      CustomResourceScanner<StackGresProfile> scanner,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler,
      AbstractResourceTransformer<ProfileDto, StackGresProfile> transformer) {
    return new ProfileResource(scanner, finder, scheduler, transformer);
  }

  @Override
  protected String getResourceNamespace() {
    return "default";
  }

  @Override
  protected String getResourceName() {
    return "size-s";
  }

  @Override
  protected void checkDto(ProfileDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("size-s", resource.getMetadata().getName());
    assertEquals("44f1f832-37a0-4346-9876-be4a9135dca5", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("1", resource.getSpec().getCpu());
    assertEquals("2Gi", resource.getSpec().getMemory());
  }

  @Override
  protected void checkCustomResource(StackGresProfile resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("size-s", resource.getMetadata().getName());
    assertEquals("44f1f832-37a0-4346-9876-be4a9135dca5", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("1", resource.getSpec().getCpu());
    assertEquals("2Gi", resource.getSpec().getMemory());
  }

}