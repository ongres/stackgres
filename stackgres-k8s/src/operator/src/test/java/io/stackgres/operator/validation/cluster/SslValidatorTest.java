/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SslValidatorTest {

  private SslValidator validator;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  private final Secret secret1 = new SecretBuilder()
      .withNewMetadata()
      .withName("secret1")
      .withNamespace("default")
      .endMetadata()
      .withData(ImmutableMap.of("test", "test"))
      .build();

  private final Secret secret2 = new SecretBuilder()
      .withNewMetadata()
      .withName("secret1")
      .withNamespace("default")
      .endMetadata()
      .withData(ImmutableMap.of("test", "test"))
      .build();

  @BeforeEach
  void setUp() {
    validator = new SslValidator(secretFinder);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();

    when(secretFinder.findByNameAndNamespace("secret1", "default"))
        .thenReturn(Optional.of(secret1));
    when(secretFinder.findByNameAndNamespace("secret2", "default"))
        .thenReturn(Optional.of(secret2));

    prepareForSsl(review);

    validator.validate(review);

    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret1", "default");
    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret2", "default");
  }

  @Test
  void givenAnUpdate_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();

    when(secretFinder.findByNameAndNamespace("secret1", "default"))
        .thenReturn(Optional.of(secret1));
    when(secretFinder.findByNameAndNamespace("secret2", "default"))
        .thenReturn(Optional.of(secret2));

    validator.validate(review);

    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret1", "default");
    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret2", "default");
  }

  @Test
  void givenACreationUsingCertificateFromNonexistentSecret_shouldFail() {
    final StackGresClusterReview review = getCreationReview();

    prepareForSsl(review);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Certificate Secret secret1 or key test not found");

    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret1", "default");
    verify(secretFinder, times(0))
        .findByNameAndNamespace("secret2", "default");
  }

  @Test
  void givenACreationUsingCertificateFromNonexistentSecretKey_shouldFail() {
    final StackGresClusterReview review = getCreationReview();

    when(secretFinder.findByNameAndNamespace("secret1", "default"))
        .thenReturn(Optional.of(secret1));

    prepareForSsl(review);
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .getCertificateSecretKeySelector().setKey("test1");

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Certificate Secret secret1 or key test1 not found");

    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret1", "default");
    verify(secretFinder, times(0))
        .findByNameAndNamespace("secret2", "default");
  }

  @Test
  void givenACreationUsingPrivateKeyFromNonexistentSecret_shouldFail() {
    final StackGresClusterReview review = getCreationReview();

    when(secretFinder.findByNameAndNamespace("secret1", "default"))
        .thenReturn(Optional.of(secret1));

    prepareForSsl(review);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Private key Secret secret2 or key test not found");

    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret1", "default");
    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret2", "default");
  }

  @Test
  void givenACreationUsingPrivateKeyFromNonexistentSecretKey_shouldFail() {
    final StackGresClusterReview review = getCreationReview();

    when(secretFinder.findByNameAndNamespace("secret1", "default"))
        .thenReturn(Optional.of(secret1));
    when(secretFinder.findByNameAndNamespace("secret1", "default"))
        .thenReturn(Optional.of(secret2));

    prepareForSsl(review);
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .getPrivateKeySecretKeySelector().setKey("test1");

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Private key Secret secret2 or key test1 not found");

    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret1", "default");
    verify(secretFinder, times(1))
        .findByNameAndNamespace("secret2", "default");
  }

  private StackGresClusterReview getCreationReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  private StackGresClusterReview getUpdateReview() {
    return AdmissionReviewFixtures.cluster().loadSslUpdate().get();
  }

  private void prepareForSsl(StackGresClusterReview review) {
    review.getRequest().getObject().getSpec().setPostgres(new StackGresClusterPostgres());
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(true);
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .getCertificateSecretKeySelector().setName("secret1");
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .getCertificateSecretKeySelector().setKey("test");
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .setPrivateKeySecretKeySelector(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .getPrivateKeySecretKeySelector().setName("secret2");
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .getPrivateKeySecretKeySelector().setKey("test");
  }

}
