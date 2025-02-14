/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.configuration.WebApiProperty;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.user.UserDto;
import io.stackgres.apiweb.rest.user.UserResource;
import io.stackgres.apiweb.security.TokenUtils;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class UserResourceQuarkusTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  private String namespace;
  private Secret user;
  private RoleBinding roleBinding;
  private ClusterRoleBinding clusterRoleBinding;
  private UserDto userDto;

  @BeforeEach
  void setUp() {
    namespace = WebApiProperty.RESTAPI_NAMESPACE.getString();
    mockServer.getClient().secrets().inNamespace(namespace).delete();
    mockServer.getClient().rbac().roleBindings().inAnyNamespace().delete();
    mockServer.getClient().rbac().clusterRoleBindings().delete();

    userDto = DtoFixtures.user().loadDefault().get();
    userDto.getMetadata().setNamespace(namespace);
    userDto.getMetadata().setName(StringUtils.getRandomResourceName());
    userDto.setPassword(StringUtils.getRandomResourceName());
    user = Fixtures.secret().loadUser().get();
    user.getMetadata().setNamespace(namespace);
    user.getMetadata().setName(userDto.getMetadata().getName());
    user.getData().put(
        StackGresContext.REST_PASSWORD_KEY,
        ResourceUtil.encodeSecret(TokenUtils.sha256(
            Optional.ofNullable(userDto.getApiUsername())
                .orElse(userDto.getK8sUsername())
                + userDto.getPassword())));
    roleBinding = new RoleBindingBuilder()
        .withNewMetadata()
        .withNamespace("test")
        .withName(UserResource.getRoleName(userDto, "test"))
        .withLabels(Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withNewRoleRef()
        .withApiGroup(HasMetadata.getGroup(Role.class))
        .withKind(HasMetadata.getKind(Role.class))
        .withName("test")
        .endRoleRef()
        .withSubjects(List.of(new SubjectBuilder()
            .withApiGroup("rbac.authorization.k8s.io")
            .withKind("User")
            .withName(userDto.getK8sUsername())
            .build()))
        .build();
    clusterRoleBinding = new ClusterRoleBindingBuilder()
        .withNewMetadata()
        .withName(UserResource.getRoleName(userDto, "test"))
        .withLabels(Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withNewRoleRef()
        .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
        .withKind(HasMetadata.getKind(ClusterRole.class))
        .withName("test")
        .endRoleRef()
        .withSubjects(List.of(new SubjectBuilder()
            .withApiGroup("rbac.authorization.k8s.io")
            .withKind("User")
            .withName(userDto.getK8sUsername())
            .build()))
        .build();
  }

  @AfterEach
  void tearDown() {
    mockServer.getClient().secrets().inNamespace(namespace).delete();
    mockServer.getClient().rbac().roleBindings().inAnyNamespace().delete();
    mockServer.getClient().rbac().clusterRoleBindings().delete();
  }

  @Test
  void givenACreation_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/users")
        .then()
        .statusCode(200)
        .body("metadata.namespace", equalTo(namespace),
            "metadata.name", equalTo(userDto.getMetadata().getName()),
            "k8sUsername", equalTo(userDto.getK8sUsername()),
            "password", nullValue(),
            "roles[0].namespace", equalTo("test"),
            "roles[0].name", equalTo("test"),
            "clusterRoles[0].name", equalTo("test"));
    checkUser();
    checkRoleBindings();
    checkClusterRoleBindings();
  }

  @Test
  void givenACreationWithApiUsername_shouldNotFail() {
    this.userDto.setApiUsername(StringUtils.getRandomString());
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/users")
        .then()
        .statusCode(200)
        .body("metadata.namespace", equalTo(namespace),
            "metadata.name", equalTo(userDto.getMetadata().getName()),
            "k8sUsername", equalTo(userDto.getK8sUsername()),
            "apiUsername", equalTo(userDto.getApiUsername()),
            "password", nullValue(),
            "roles[0].namespace", equalTo("test"),
            "roles[0].name", equalTo("test"),
            "clusterRoles[0].name", equalTo("test"));
    checkUser();
    checkRoleBindings();
    checkClusterRoleBindings();
  }

  @Test
  void givenACreationWithExistingBindings_shouldNotFail() {
    mockServer.getClient()
        .resource(roleBinding)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(clusterRoleBinding)
        .createOr(Replaceable::update);
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/users")
        .then()
        .statusCode(200)
        .body("metadata.namespace", equalTo(namespace),
            "metadata.name", equalTo(userDto.getMetadata().getName()),
            "k8sUsername", equalTo(userDto.getK8sUsername()),
            "password", nullValue(),
            "roles[0].namespace", equalTo("test"),
            "roles[0].name", equalTo("test"),
            "clusterRoles[0].name", equalTo("test"));
    checkUser();
    checkRoleBindings();
    checkClusterRoleBindings();
  }

  @Test
  void givenAnUpdate_shouldNotFail() {
    mockServer.getClient()
        .resource(user)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(roleBinding)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(clusterRoleBinding)
        .createOr(Replaceable::update);
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .put("/stackgres/users")
        .then()
        .statusCode(200)
        .body("metadata.namespace", equalTo(namespace),
            "metadata.name", equalTo(userDto.getMetadata().getName()),
            "k8sUsername", equalTo(userDto.getK8sUsername()),
            "password", nullValue(),
            "roles[0].namespace", equalTo("test"),
            "roles[0].name", equalTo("test"),
            "clusterRoles[0].name", equalTo("test"));
    checkUser();
    checkRoleBindings();
    checkClusterRoleBindings();
  }

  @Test
  void givenAnUpdateWithoutPassword_shouldNotFail() {
    mockServer.getClient()
        .resource(user)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(roleBinding)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(clusterRoleBinding)
        .createOr(Replaceable::update);
    var userDto = JsonUtil.copy(this.userDto);
    userDto.setPassword(null);
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .put("/stackgres/users")
        .then()
        .statusCode(200)
        .body("metadata.namespace", equalTo(namespace),
            "metadata.name", equalTo(userDto.getMetadata().getName()),
            "k8sUsername", equalTo(userDto.getK8sUsername()),
            "password", nullValue(),
            "roles[0].namespace", equalTo("test"),
            "roles[0].name", equalTo("test"),
            "clusterRoles[0].name", equalTo("test"));
    checkUser();
    checkRoleBindings();
    checkClusterRoleBindings();
  }

  @Test
  void givenAnUpdateWithoutApiUsername_shouldNotFail() {
    mockServer.getClient()
        .resource(user)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(roleBinding)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(clusterRoleBinding)
        .createOr(Replaceable::update);
    this.userDto.setApiUsername(StringUtils.getRandomString());
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .put("/stackgres/users")
        .then()
        .statusCode(200)
        .body("metadata.namespace", equalTo(namespace),
            "metadata.name", equalTo(userDto.getMetadata().getName()),
            "k8sUsername", equalTo(userDto.getK8sUsername()),
            "apiUsername", equalTo(userDto.getApiUsername()),
            "password", nullValue(),
            "roles[0].namespace", equalTo("test"),
            "roles[0].name", equalTo("test"),
            "clusterRoles[0].name", equalTo("test"));
    checkUser();
    checkRoleBindings();
    checkClusterRoleBindings();
  }

  @Test
  void givenADeletionWithoutPassword_shouldNotFail() {
    mockServer.getClient()
        .resource(user)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(roleBinding)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(clusterRoleBinding)
        .createOr(Replaceable::update);
    var userDto = JsonUtil.copy(this.userDto);
    userDto.setPassword(null);
    given()
        .header(AUTHENTICATION_HEADER)
        .body(userDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .delete("/stackgres/users")
        .then()
        .statusCode(204);
    var user = mockServer.getClient().secrets()
        .inNamespace(namespace)
        .withName(userDto.getMetadata().getName())
        .get();
    assertNull(user);
    var roleBindings = mockServer.getClient().rbac().roleBindings().inAnyNamespace().list();
    assertEquals(0, roleBindings.getItems().size());
    var clusterRoleBindings = mockServer.getClient().rbac().clusterRoleBindings().list();
    assertEquals(0, clusterRoleBindings.getItems().size());
  }

  @Test
  void getListUser_shouldNotFail() {
    mockServer.getClient()
        .resource(user)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(roleBinding)
        .createOr(Replaceable::update);
    mockServer.getClient()
        .resource(clusterRoleBinding)
        .createOr(Replaceable::update);
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/users")
        .then()
        .statusCode(200)
        .body("[0].metadata.namespace", equalTo(namespace),
            "[0].metadata.name", equalTo(userDto.getMetadata().getName()),
            "[0].k8sUsername", equalTo(userDto.getK8sUsername()),
            "[0].password", nullValue(),
            "[0].roles[0].namespace", equalTo("test"),
            "[0].roles[0].name", equalTo("test"),
            "[0].clusterRoles[0].name", equalTo("test"));
  }

  private void checkUser() {
    var user = mockServer.getClient().secrets()
        .inNamespace(namespace)
        .withName(userDto.getMetadata().getName())
        .get();
    assertEquals(
        Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE),
        user.getMetadata().getLabels());
    assertEquals(
        Optional.ofNullable(userDto.getApiUsername()).map(ResourceUtil::encodeSecret).orElse(null),
        user.getData().get(StackGresContext.REST_APIUSER_KEY));
    assertEquals(
        ResourceUtil.encodeSecret(userDto.getK8sUsername()),
        user.getData().get(StackGresContext.REST_K8SUSER_KEY));
    assertEquals(
        ResourceUtil.encodeSecret(TokenUtils.sha256(
            Optional.ofNullable(userDto.getApiUsername())
                .orElse(userDto.getK8sUsername())
                + userDto.getPassword())),
        user.getData().get(StackGresContext.REST_PASSWORD_KEY));
  }

  private void checkRoleBindings() {
    var roleBindings = mockServer.getClient().rbac().roleBindings().inAnyNamespace().list();
    assertEquals(1, roleBindings.getItems().size());
    assertEquals(
        this.roleBinding.getMetadata().getName(),
        roleBindings.getItems().get(0).getMetadata().getName());
    assertEquals(
        this.roleBinding.getMetadata().getNamespace(),
        roleBindings.getItems().get(0).getMetadata().getNamespace());
    assertEquals(
        this.roleBinding.getRoleRef(),
        roleBindings.getItems().get(0).getRoleRef());
    assertEquals(
        this.roleBinding.getSubjects(),
        roleBindings.getItems().get(0).getSubjects());
  }

  private void checkClusterRoleBindings() {
    var clusterRoleBindings = mockServer.getClient().rbac().clusterRoleBindings().list();
    assertEquals(1, clusterRoleBindings.getItems().size());
    assertEquals(
        this.clusterRoleBinding.getMetadata().getName(),
        clusterRoleBindings.getItems().get(0).getMetadata().getName());
    assertEquals(
        this.clusterRoleBinding.getMetadata().getNamespace(),
        clusterRoleBindings.getItems().get(0).getMetadata().getNamespace());
    assertEquals(
        this.clusterRoleBinding.getRoleRef(),
        clusterRoleBindings.getItems().get(0).getRoleRef());
    assertEquals(
        this.clusterRoleBinding.getSubjects(),
        clusterRoleBindings.getItems().get(0).getSubjects());
  }

}
