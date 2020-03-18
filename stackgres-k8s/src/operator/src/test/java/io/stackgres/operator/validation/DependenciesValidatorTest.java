/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class DependenciesValidatorTest<T extends AdmissionReview<?>, V extends DependenciesValidator<T>> {

  protected DependenciesValidator<T> validator;

  @Mock
  protected CustomResourceScanner<StackGresCluster> clusterScanner;

  protected abstract DependenciesValidator<T> setUpValidation();

  @BeforeEach
  void setUp() {
    validator = setUpValidation();
    validator.setClusterScanner(clusterScanner);
    validator.init(new ConfigLoader());
  }

  @Test
  protected abstract void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed;

  protected void givenAReviewCreation_itShouldDoNothing(T review) throws ValidationFailed {

    validator.validate(review);

    verify(clusterScanner, never()).findResources();
    verify(clusterScanner, never()).findResources(anyString());

  }

  @Test
  protected abstract void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed;

  protected void givenAReviewUpdate_itShouldDoNothing(T review) throws ValidationFailed {
    validator.validate(review);

    verify(clusterScanner, never()).findResources();
    verify(clusterScanner, never()).findResources(anyString());
  }

  @Test
  protected abstract void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt();

  protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(T review) {

    StackGresClusterList clusterList = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class);

    when(clusterScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.of(clusterList.getItems()));

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.FORBIDDEN_CR_DELETION,
        () -> validator.validate(review));

    assertEquals("Can't delete "
            + review.getRequest().getResource().getResource()
            + "." + review.getRequest().getKind().getGroup()
            + " " + review.getRequest().getName() + " because the "
            + StackGresClusterDefinition.NAME + " "
            + clusterList.getItems().get(0).getMetadata().getName() + " depends on it"
        , ex.getResult().getMessage());

  }

  @Test
  protected abstract void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed;

  protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(T review) throws ValidationFailed {

    when(clusterScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.empty());

    validator.validate(review);

    verify(clusterScanner, never()).findResources();
    verify(clusterScanner).findResources(review.getRequest().getNamespace());

  }
}
