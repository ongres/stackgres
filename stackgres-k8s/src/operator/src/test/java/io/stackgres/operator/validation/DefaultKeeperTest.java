/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class DefaultKeeperTest
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>> {

  @Mock
  private DefaultCustomResourceHolder<R> defaultCustomResourceHolder;

  private AbstractDefaultConfigKeeper<R, T> validator;

  @BeforeEach
  void setUp() {
    validator = getValidatorInstance();
    validator.setDefaultCustomResourceHolder(defaultCustomResourceHolder);
  }

  protected abstract AbstractDefaultConfigKeeper<R, T> getValidatorInstance();

  protected abstract T getCreationSample();

  protected abstract T getDeleteSample();

  protected abstract T getUpdateSample();

  @Test
  void givenACreationSample_ItShouldNotFail() throws ValidationFailed {

    T sample = getCreationSample();

    validator.validate(sample);

    verify(defaultCustomResourceHolder, never()).isDefaultCustomResource(any());
    verify(defaultCustomResourceHolder, never()).isDefaultCustomResource(anyString(), anyString());

  }

  @Test
  void givenAnUpdateSampleToANonDefaultResource_ItShouldNotFail()
      throws ValidationFailed {

    T sample = getUpdateSample();

    when(defaultCustomResourceHolder.isDefaultCustomResource(sample.getRequest().getObject()))
        .thenReturn(false);

    validator.validate(sample);

    verify(defaultCustomResourceHolder).isDefaultCustomResource(any());

  }

  @Test
  void givenAnUpdateOnTheDefaultResource_ShouldFail() {

    T sample = getUpdateSample();

    when(defaultCustomResourceHolder.isDefaultCustomResource(sample.getRequest().getObject()))
        .thenReturn(true);

    ValidationUtils.assertErrorType(ErrorType.DEFAULT_CONFIGURATION,
        () -> validator.validate(sample));

    verify(defaultCustomResourceHolder).isDefaultCustomResource(any());
  }

  @Test
  void givenAnDeleteSampleToNonDefaultResource_ItShouldNotFail()
      throws ValidationFailed {

    T sample = getDeleteSample();

    when(defaultCustomResourceHolder.isDefaultCustomResource(
        sample.getRequest().getName(),
        sample.getRequest().getNamespace()))
        .thenReturn(false);
    validator.validate(sample);

    verify(defaultCustomResourceHolder).isDefaultCustomResource(anyString(), anyString());

  }

  @Test
  void givenAnDeleteOnTheDefaultResource_ShouldFail() {

    T sample = getDeleteSample();

    when(defaultCustomResourceHolder.isDefaultCustomResource(anyString(), anyString()))
        .thenReturn(true);

    ValidationUtils.assertErrorType(ErrorType.DEFAULT_CONFIGURATION,
        () -> validator.validate(sample));

  }

}
