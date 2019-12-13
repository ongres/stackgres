/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Random;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.AdmissionReview;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public abstract class DefaultKeeperTest<R extends CustomResource, T extends AdmissionReview<R>> {

  @Mock
  private DefaultCustomResourceFactory<R> factory;

  private AbstractDefaultConfigKeeper<R, T> validator;

  @BeforeEach
  void setUp() {
    validator = getValidatorInstance();
    validator.setFactory(factory);
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
    assertThrows(ValidationFailed.class, () -> {
      validator.validate(sample);
    });

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

    assertThrows(ValidationFailed.class, () -> {
      validator.validate(sample);
    });

  }

  private static String getRandomString(){
    int length = new Random().nextInt(128) + 1;
    byte[] stringBuffer = new byte[length];
    return new String(stringBuffer);
  }

}
