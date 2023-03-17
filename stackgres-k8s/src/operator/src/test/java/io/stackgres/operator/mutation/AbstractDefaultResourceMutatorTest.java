/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.io.IOException;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractDefaultResourceMutatorTest<
        C extends CustomResource<?, ?>, T extends CustomResource<?, ?>,
        R extends AdmissionReview<T>, M extends AbstractDefaultResourceMutator<C, T, R>> {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  protected R review;

  protected M mutator;

  @Mock
  protected CustomResourceFinder<C> finder;

  @Mock
  protected CustomResourceScheduler<C> scheduler;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = getAdmissionReview();

    mutator = getDefaultConfigMutator();
  }

  protected abstract R getAdmissionReview();

  protected abstract M getDefaultConfigMutator();

  protected abstract Class<T> getResourceClass();

  protected abstract void checkConfigurationIsSet(T newResource);

  @Test
  protected void clusterWithExistingConfiguration_shouldNotDoAnything() {
    setUpExistingConfiguration();

    T result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    Assertions.assertEquals(review.getRequest().getObject(), result);
  }

  protected abstract void setUpExistingConfiguration();

  @Test
  protected void clusteWithNoConfiguration_shouldSetOne() throws Exception {
    setUpMissingConfiguration();

    T result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    Assertions.assertNotEquals(review.getRequest().getObject(), result);

    checkConfigurationIsSet(result);
  }

  protected abstract void setUpMissingConfiguration();

  @Test
  protected void clusteWithNoConfigurationSection_shouldSetOne() throws Exception {
    setUpMissingConfigurationSection();

    T result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    Assertions.assertNotEquals(review.getRequest().getObject(), result);

    checkConfigurationIsSet(result);
  }

  protected abstract void setUpMissingConfigurationSection();

}
