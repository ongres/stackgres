/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.RandomObjectUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
class ObjectStorageValidationResourceTest {

  @Test
  @DisplayName("Given a valid creation review should pass")
  void testValidCreate() {
    var review = getValidReview();

    given()
        .body(review)
        .contentType(ContentType.JSON)
        .post(ValidationUtil.OBJECT_STORAGE_VALIDATION_PATH)
        .then()
        .statusCode(200)
        .body("response.allowed", is(true));

  }

  private StackGresObjectStorageReview getValidReview() {
    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();
    var objectStorage = review.getRequest().getObject();
    var backupStorage = objectStorage.getSpec();
    backupStorage.setType("s3");
    backupStorage.setGcs(null);
    backupStorage.setAzureBlob(null);
    backupStorage.setS3Compatible(null);
    var s3Storage = RandomObjectUtils.generateRandomObject(AwsS3Storage.class);
    backupStorage.setS3(s3Storage);
    s3Storage.setStorageClass("REDUCED_REDUNDANCY");

    return review;
  }

}
