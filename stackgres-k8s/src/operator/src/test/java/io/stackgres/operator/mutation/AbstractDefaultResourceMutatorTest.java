/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.testutil.JsonUtil;
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
  void clusterWithExistingConfiguration_shouldNotDoAnything() {
    setUpExistingConfiguration();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());
  }

  protected abstract void setUpExistingConfiguration();

  @Test
  void clusteWithNoConfiguration_shouldSetOne() throws Exception {
    setUpMissingConfiguration();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    T newResource = JSON_MAPPER.treeToValue(jp.apply(crJson), getResourceClass());

    checkConfigurationIsSet(newResource);
  }

  protected abstract void setUpMissingConfiguration();

  @Test
  void clusteWithNoConfigurationSection_shouldSetOne() throws Exception {
    setUpMissingConfigurationSection();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    T newResource = JSON_MAPPER.treeToValue(jp.apply(crJson), getResourceClass());

    checkConfigurationIsSet(newResource);
  }

  protected abstract void setUpMissingConfigurationSection();

}
