/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import java.util.Map;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.ErrorType;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.resource.SecretWriter;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.RandomObjectUtils;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ObjectStorageValidationPipelineTest {

  @Inject
  ObjectStorageValidationPipeline pipeline;

  @Inject
  SecretWriter writer;

  @Test
  @DisplayName("Valid reviews should pass")
  void testValidReview() throws ValidationFailed {

    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    var objectStorage = review.getRequest().getObject();
    var backupStorage = objectStorage.getSpec();
    backupStorage.setType("s3");
    backupStorage.setGcs(null);
    backupStorage.setAzureBlob(null);
    backupStorage.setS3Compatible(null);
    var s3Storage = RandomObjectUtils.generateRandomObject(AwsS3Storage.class);
    backupStorage.setS3(s3Storage);

    var accessKeyId = s3Storage.getAwsCredentials().getSecretKeySelectors()
        .getAccessKeyId();
    var secretAccessKey = s3Storage.getAwsCredentials().getSecretKeySelectors()
        .getSecretAccessKey();
    secretAccessKey.setName(accessKeyId.getName());

    s3Storage.setStorageClass(null);

    writer.create(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(objectStorage.getMetadata().getNamespace())
        .withName(accessKeyId.getName())
        .endMetadata()
        .withData(
            Map.of(
                accessKeyId.getKey(), StringUtils.getRandomString(),
                secretAccessKey.getKey(), StringUtils.getRandomString()))
        .build());

    pipeline.validate(review);
  }

  @Test
  @DisplayName("Given an invalid creation should fail")
  void testConstraintValidations() {
    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();
    var objectStorage = review.getRequest().getObject();
    objectStorage.setSpec(
        RandomObjectUtils.generateRandomObject(BackupStorage.class));

    ValidationUtils.assertErrorType(
        ErrorType.CONSTRAINT_VIOLATION, () -> pipeline.validate(review));
  }
}
