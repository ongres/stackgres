/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.ProfileTransformer;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileResourceTest extends AbstractDependencyCustomResourceTest
      <ProfileDto, StackGresProfile, ProfileResource, NamespacedProfileResource> {

  @Override
  protected DefaultKubernetesResourceList<StackGresProfile> getCustomResourceList() {
    return Fixtures.instanceProfileList().loadDefault().get();
  }

  @Override
  protected ProfileDto getResourceDto() {
    return DtoFixtures.instanceProfile().loadDefault().get();
  }

  @Override
  protected AbstractDependencyResourceTransformer<ProfileDto, StackGresProfile> getTransformer() {
    return new ProfileTransformer(
        JsonMapper.builder().build()
    );
  }

  @Override
  protected ProfileResource getService() {
    return new ProfileResource();
  }

  @Override
  protected NamespacedProfileResource getNamespacedService() {
    return new NamespacedProfileResource();
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "size-s";
  }

  @Override
  protected void checkDto(ProfileDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("size-s", resource.getMetadata().getName());
    assertEquals("44f1f832-37a0-4346-9876-be4a9135dca5", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("1", resource.getSpec().getCpu());
    assertEquals("2Gi", resource.getSpec().getMemory());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(2, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
  }

  @Override
  protected void checkCustomResource(StackGresProfile resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("size-s", resource.getMetadata().getName());
    assertEquals("44f1f832-37a0-4346-9876-be4a9135dca5", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("1", resource.getSpec().getCpu());
    assertEquals("2Gi", resource.getSpec().getMemory());
  }

}
