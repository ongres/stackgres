/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.V1AuthorizationAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.AuthorizationAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.InOutCreateable;
import io.quarkus.security.identity.SecurityIdentity;
import io.stackgres.apiweb.app.KubernetesClientProvider;
import io.stackgres.apiweb.dto.PermissionsListDto;
import io.stackgres.apiweb.dto.PermissionsListDto.Namespaced;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RbacResourceTest {

  @Mock
  SecurityIdentity identity;

  @Mock
  Principal principal;

  @Mock
  NamespaceResource namespaces;

  @Mock
  KubernetesClientProvider kubernetesClientProvider;

  @Mock
  KubernetesClient k8sClient;

  @Mock
  SubjectAccessReview review;

  @InjectMocks
  RbacResource rbacResource = new RbacResource();

  @Mock
  private AuthorizationAPIGroupDSL authorization;

  @Mock
  private V1AuthorizationAPIGroupDSL v1AuthorizationApiGrouDsl;

  @Mock
  private InOutCreateable<SubjectAccessReview, SubjectAccessReview> inOutSubjectReview;

  @Mock
  private SubjectAccessReviewStatus subjectReviewStatus;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    given(kubernetesClientProvider.createDefault()).willReturn(k8sClient);
    given(k8sClient.authorization()).willReturn(authorization);
    given(authorization.v1()).willReturn(v1AuthorizationApiGrouDsl);
    given(v1AuthorizationApiGrouDsl.subjectAccessReview()).willReturn(inOutSubjectReview);
    given(inOutSubjectReview.create(any(SubjectAccessReview.class))).willReturn(review);
    given(review.getStatus()).willReturn(subjectReviewStatus);
    given(identity.getPrincipal()).willReturn(principal);
    given(identity.getPrincipal().getName()).willReturn("admin");
  }

  @Test
  void shouldCanIAccessResource_onceMyRoleHasPermission() {
    given(subjectReviewStatus.getAllowed()).willReturn(true);

    Response canIGetPodsExec =
        rbacResource.verb("GET", "pods", "sgcluster-0", Optional.of("group"));
    assertEquals(Response.Status.OK.getStatusCode(), canIGetPodsExec.getStatus());
  }

  @Test
  void shouldNotCanIAccessSubResource_onceMyRoleHasNotPermission() {
    given(subjectReviewStatus.getAllowed()).willReturn(false);

    Response canIGetPodsExec =
        rbacResource.verb("GET", "pods/logs", "sgcluster-0", Optional.of("group"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), canIGetPodsExec.getStatus());
  }

  @Test
  void shouldListUnnamespacedAndNamespacedResources() {
    given(review.getStatus()).willReturn(subjectReviewStatus);
    given(subjectReviewStatus.getAllowed()).willReturn(true);
    given(namespaces.get()).willReturn(expectedNamespaces());

    Response permissions = rbacResource.caniList();

    assertEquals(Response.Status.OK.getStatusCode(), permissions.getStatus());
    var entity = ((PermissionsListDto) permissions.getEntity());
    assertUnnamespacedResources(entity.unnamespaced());
    assertNamespacedResources(entity.namespaced());
  }

  private List<String> expectedNamespaces() {
    return Arrays.asList("sgcluster");
  }

  private void assertNamespacedResources(List<Namespaced> actualNamespaced) {
    assertEquals(expectedNamespaces().size(), actualNamespaced.size());
    assertEquals(expectedNamespaces().iterator().next(),
        actualNamespaced.iterator().next().namespace());

    Map<String, List<String>> resources = actualNamespaced.iterator().next().resources();
    for (Map.Entry<String, List<String>> resource : resources.entrySet()) {
      assertNamespacedResource(resource.getKey());
    }
  }

  private void assertNamespacedResource(String actualResourceName) {
    expectedNamespacedResourceNames().contains(actualResourceName);
  }

  private List<String> expectedNamespacedResourceNames() {
    return rbacResource.getResourcesNamespaced();
  }

  private void assertUnnamespacedResources(Map<String, List<String>> unnamespaced) {
    assertEquals(expectedUnnamespaces().size(), unnamespaced.size());
    for (Map.Entry<String, List<String>> resource : unnamespaced.entrySet()) {
      assertUnnamespacedResource(resource.getKey());
    }
  }

  private void assertUnnamespacedResource(String unnamespacedResource) {
    expectedUnnamespaces().contains(unnamespacedResource);
  }

  private List<String> expectedUnnamespaces() {
    return rbacResource.getResourcesUnnamespaced();
  }

}
