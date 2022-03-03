/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.resource.SecretWriter;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.RandomObjectUtils;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
class ObjectStorageValidationResourceTest {

  @Inject
  SecretWriter writer;

  @Test
  @DisplayName("Given a valid creation review should pass")
  void testValidCreate() {
    var review = getValidReview();
    createMandatorySecrets(review);

    given()
        .body(review)
        .contentType(ContentType.JSON)
        .post(ValidationUtil.OBJECT_STORAGE_VALIDATION_PATH)
        .then()
        .statusCode(200)
        .body("response.allowed", is(true));

  }

  @Test
  @DisplayName("Given a valid creation review should fail")
  void testInvalidCreated() {
    var review = getValidReview();

    given()
        .body(review)
        .post(ValidationUtil.OBJECT_STORAGE_VALIDATION_PATH)
        .then()
        .statusCode(200)
        .body("response.allowed", is(false));
  }

  private ObjectStorageReview getValidReview() {
    ObjectStorageReview review = JsonUtil.readFromJson(
        "objectstorage_allow_request/create.json", ObjectStorageReview.class);
    var objectStorage = review.getRequest().getObject();
    var backupStorage = objectStorage.getSpec();
    backupStorage.setType("s3");
    backupStorage.setGcs(null);
    backupStorage.setAzureBlob(null);
    backupStorage.setS3Compatible(null);
    var s3Storage = RandomObjectUtils.generateRandomObject(AwsS3Storage.class);
    backupStorage.setS3(s3Storage);

    return review;
  }

  private void createMandatorySecrets(ObjectStorageReview review) {
    var objectStorage = review.getRequest().getObject();
    final AwsSecretKeySelector secretKeySelectors = objectStorage.getSpec().getS3()
        .getAwsCredentials().getSecretKeySelectors();
    var accessKeyId = secretKeySelectors.getAccessKeyId();
    var secretAccessKey = secretKeySelectors.getSecretAccessKey();

    createSecrets(
        objectStorage.getMetadata().getNamespace(),
        accessKeyId, secretAccessKey
    );

  }

  private void createSecrets(String namespace, SecretKeySelector... selectors) {

    Arrays.stream(selectors).forEach(selector -> {
      writer.create(
          new SecretBuilder()
              .withNewMetadata()
              .withNamespace(namespace)
              .withName(selector.getName())
              .endMetadata()
              .withData(
                  Map.of(
                      selector.getKey(), StringUtils.getRandomString()
                  )
              )
              .build());
    });

  }

}
