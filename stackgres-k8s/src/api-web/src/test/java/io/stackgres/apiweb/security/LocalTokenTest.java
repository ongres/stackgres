
package io.stackgres.apiweb.security;

import static io.restassured.RestAssured.given;

import java.util.Map;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.rest.LocalLoginResource;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(LocalLoginResource.class)
class LocalTokenTest {

  @Inject
  KubernetesClientFactory factory;

  String namespace = WebApiProperty.RESTAPI_NAMESPACE.getString();

  @BeforeEach
  void setupSecret() {
    Secret demo_user = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName("demo-user")
        .withLabels(Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withType("Opaque")
        .withData(Map.of(
            StackGresContext.REST_APIUSER_KEY, ResourceUtil.encodeSecret("apiuser"),
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("stackgres"),
            StackGresContext.REST_PASSWORD_KEY,
            ResourceUtil.encodeSecret(TokenUtils.sha256("apiuserdemo123"))))
        .build();
    Secret demo_user_no_labels = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName("demo-user-no-labels")
        .endMetadata()
        .withType("Opaque")
        .withData(Map.of(
            StackGresContext.REST_APIUSER_KEY, ResourceUtil.encodeSecret("apiuser-nolabel"),
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("apiuser-nolabel"),
            StackGresContext.REST_PASSWORD_KEY,
            ResourceUtil.encodeSecret(TokenUtils.sha256("apiuser-nolabeldemo123"))))
        .build();
    Secret demo_user_no_apiuser = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName("demo-user-no-apiuser")
        .withLabels(Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withType("Opaque")
        .withData(Map.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("k8suser"),
            StackGresContext.REST_PASSWORD_KEY,
            ResourceUtil.encodeSecret(TokenUtils.sha256("k8suserdemo123"))))
        .build();

    try (KubernetesClient client = factory.create()) {
      client.secrets().inNamespace(namespace)
          .createOrReplace(demo_user);
      client.secrets().inNamespace(namespace)
          .createOrReplace(demo_user_no_labels);
      client.secrets().inNamespace(namespace)
          .createOrReplace(demo_user_no_apiuser);
    }
  }

  @AfterEach
  void dropSecret() {
    try (KubernetesClient client = factory.create()) {
      client.secrets().inNamespace(namespace)
          .withName("demo-user").delete();
      client.secrets().inNamespace(namespace)
          .withName("demo-user-no-labels").delete();
      client.secrets().inNamespace(namespace)
          .withName("demo-user-no-apiuser").delete();
    }
  }

  @Test
  void try_login_success_apiuser() {
    UserPassword up = new UserPassword();
    up.setUserName("apiuser");
    up.setPassword("demo123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/login")
        .then()
        .statusCode(200)
        .body("access_token", Matchers.startsWith("eyJ"),
            "token_type", Matchers.is("Bearer"),
            "expires_in", Matchers.is(28800));
  }

  @Test
  void try_login_success_k8suser() {
    UserPassword up = new UserPassword();
    up.setUserName("k8suser");
    up.setPassword("demo123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/login")
        .then()
        .statusCode(200)
        .body("access_token", Matchers.startsWith("eyJ"),
            "token_type", Matchers.is("Bearer"),
            "expires_in", Matchers.is(28800));
  }

  @Test
  void try_login_failure_no_label() {
    UserPassword up = new UserPassword();
    up.setUserName("apiuser-nolabel");
    up.setPassword("demo123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/login")
        .then()
        .statusCode(403);
  }

  @Test
  void try_login_failure_bad_passwd() {
    UserPassword up = new UserPassword();
    up.setUserName("apiuser");
    up.setPassword("123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/login")
        .then()
        .statusCode(403);
  }

}
