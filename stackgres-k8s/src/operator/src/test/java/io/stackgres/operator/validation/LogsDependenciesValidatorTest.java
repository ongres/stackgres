/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class LogsDependenciesValidatorTest
    <T extends AdmissionReview<?>,
    V extends DependenciesValidator<T, StackGresDistributedLogs>> {

  protected V validator;

  @Mock
  protected CustomResourceScanner<StackGresDistributedLogs> resourceScanner;

  protected abstract V setUpValidation();

  @BeforeEach
  void setUp() {
    validator = setUpValidation();
    validator.setResourceScanner(resourceScanner);
  }

  @Test
  public void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {
    T review = getReview_givenAReviewCreation_itShouldDoNothing();

    validator.validate(review);

    verify(resourceScanner, never()).findResources();
    verify(resourceScanner, never()).findResources(anyString());
  }

  protected abstract T getReview_givenAReviewCreation_itShouldDoNothing();

  @Test
  public void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {
    T review = getReview_givenAReviewUpdate_itShouldDoNothing();

    validator.validate(review);

    verify(resourceScanner, never()).findResources();
    verify(resourceScanner, never()).findResources(anyString());
  }

  protected abstract T getReview_givenAReviewUpdate_itShouldDoNothing();

  @Test
  public void givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    T review = getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt();

    StackGresDistributedLogsList distributedLogsList =
        Fixtures.distributedLogsList().loadDefault().get();

    when(resourceScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.of(distributedLogsList.getItems()));

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.FORBIDDEN_CR_DELETION,
        () -> validator.validate(review));

    assertEquals("Can't delete "
        + review.getRequest().getResource().getResource()
        + "." + review.getRequest().getKind().getGroup()
        + " " + review.getRequest().getName() + " because the "
        + CustomResource.getCRDName(StackGresDistributedLogs.class) + " "
        + distributedLogsList.getItems().get(0).getMetadata().getName() + " depends on it",
        ex.getResult().getMessage());
  }

  protected abstract T getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt();

  @Test
  public void givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt() throws ValidationFailed {
    T review = getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt();

    StackGresDistributedLogsList distributedLogsList =
        Fixtures.distributedLogsList().loadDefault().get();
    distributedLogsList
        .getItems()
        .stream()
        .forEach(this::makeClusterNotDependant);

    when(resourceScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.of(distributedLogsList.getItems()));

    validator.validate(review);

    verify(resourceScanner, never()).findResources();
    verify(resourceScanner).findResources(review.getRequest().getNamespace());
  }

  protected abstract T getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed;

  protected abstract void makeClusterNotDependant(StackGresDistributedLogs distributedLogs);

  @Test
  public void givenAReviewDelete_itShouldNotFailIfNoClusterExists() throws ValidationFailed {
    T review = getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists();

    when(resourceScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.empty());

    validator.validate(review);

    verify(resourceScanner, never()).findResources();
    verify(resourceScanner).findResources(review.getRequest().getNamespace());
  }

  protected abstract T getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists();

}
