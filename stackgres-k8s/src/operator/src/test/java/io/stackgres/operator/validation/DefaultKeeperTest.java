/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Random;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class DefaultKeeperTest<R extends CustomResource, T extends AdmissionReview<R>> {

  @Mock
  private DefaultCustomResourceFactory<R> factory;

  @Mock
  private Instance<DefaultCustomResourceFactory<R>> factories;

  private AbstractDefaultConfigKeeper<R, T> validator;

  private static String getRandomString() {
    int length = new Random().nextInt(128) + 1;
    byte[] stringBuffer = new byte[length];
    return new String(stringBuffer);
  }

  @BeforeEach
  void setUp() {
    validator = getValidatorInstance();
    validator.setConfigContext(new ConfigLoader());
    when(factories.stream())
        .thenAnswer((Answer<Stream<DefaultCustomResourceFactory<R>>>) invocationOnMock
            -> Stream.of(factory));
    validator.setFactories(factories);

  }

  protected abstract AbstractDefaultConfigKeeper<R, T> getValidatorInstance();

  protected abstract T getCreationSample();

  protected abstract T getDeleteSample();

  protected abstract T getUpdateSample();

  protected abstract R getDefault();

  @Test
  void givenACreationSample_ItShouldNotFail() throws ValidationFailed {

    T sample = getCreationSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(sample.getRequest().getObject().getMetadata().getNamespace());
    defaultResource.getMetadata().setName(sample.getRequest().getObject().getMetadata().getName());

    when(factory.buildResource()).thenReturn(defaultResource);

    validator.init();
    validator.validate(sample);

  }

  @Test
  void givenAnUpdateSampleToAnotherResourceInTheSameNamespace_ItShouldNotFail() throws ValidationFailed {

    T sample = getUpdateSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(sample.getRequest().getObject().getMetadata().getNamespace());
    defaultResource.getMetadata().setName(getRandomString());

    when(factory.buildResource()).thenReturn(defaultResource);

    validator.init();
    validator.validate(sample);

  }

  @Test
  void givenAnUpdateSampleToAnotherResourceWithTheSameNameInAnotherNamespace_ItShouldNotFail() throws ValidationFailed {

    T sample = getUpdateSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(getRandomString());
    defaultResource.getMetadata().setName(sample.getRequest().getObject().getMetadata().getName());

    when(factory.buildResource()).thenReturn(defaultResource);

    validator.init();
    validator.validate(sample);

  }

  @Test
  void givenAnUpdateOnTheDefaultResource_ShouldFail() {

    T sample = getUpdateSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(sample.getRequest().getObject().getMetadata().getNamespace());
    defaultResource.getMetadata().setName(sample.getRequest().getObject().getMetadata().getName());

    when(factory.buildResource()).thenReturn(defaultResource);
    validator.init();

    ValidationUtils.assertErrorType(ErrorType.DEFAULT_CONFIGURATION,
        () -> validator.validate(sample));
  }

  @Test
  void givenAnDeleteSampleToAnotherResourceInTheSameNamespace_ItShouldNotFail() throws ValidationFailed {

    T sample = getDeleteSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(sample.getRequest().getNamespace());
    defaultResource.getMetadata().setName(getRandomString());

    when(factory.buildResource()).thenReturn(defaultResource);
    validator.init();
    validator.validate(sample);

  }

  @Test
  void givenAnDeleteSampleToAnotherResourceWithTheSameNameInAnotherNamespace_ItShouldNotFail() throws ValidationFailed {

    T sample = getDeleteSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(getRandomString());
    defaultResource.getMetadata().setName(sample.getRequest().getName());

    when(factory.buildResource()).thenReturn(defaultResource);
    validator.init();
    validator.validate(sample);

  }

  @Test
  void givenAnDeleteOnTheDefaultResource_ShouldFail() {

    T sample = getDeleteSample();
    R defaultResource = getDefault();

    defaultResource.getMetadata().setNamespace(sample.getRequest().getNamespace());
    defaultResource.getMetadata().setName(sample.getRequest().getName());

    when(factory.buildResource()).thenReturn(defaultResource);
    validator.init();

    ValidationUtils.assertErrorType(ErrorType.DEFAULT_CONFIGURATION,
        () -> validator.validate(sample));

  }

}
