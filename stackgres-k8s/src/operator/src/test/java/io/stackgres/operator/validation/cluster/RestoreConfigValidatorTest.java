/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestoreConfigValidatorTest {

  @Mock
  private KubernetesCustomResourceFinder<StackgresRestoreConfig> finder;

  private RestoreConfigValidator validator;

  private static final StackgresRestoreConfig restoreConfig = JsonUtil
      .readFromJson("restore_config/default.json", StackgresRestoreConfig.class);


  @BeforeEach
  void setUp() {
    validator = new RestoreConfigValidator(finder);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {

    final StackgresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    String namespace = cluster.getMetadata().getNamespace();
    String restoreConfig = cluster.getSpec().getRestoreConfig();

    when(finder.findByNameAndNamespace(restoreConfig, namespace))
        .thenReturn(Optional.of(RestoreConfigValidatorTest.restoreConfig));

    validator.validate(review);

    verify(finder).findByNameAndNamespace(restoreConfig, namespace);

  }

  @Test
  void givenAInvalidCreation_shouldFail(){

    final StackgresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    String namespace = review.getRequest().getNamespace();
    String restoreConfig = cluster.getSpec().getRestoreConfig();
    when(finder.findByNameAndNamespace(restoreConfig, namespace)).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Restore config " + restoreConfig + " not found");

    verify(finder).findByNameAndNamespace(restoreConfig, namespace);

  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {

    final StackgresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().setRestoreConfig(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void givenAnUpdate_shouldFail() {

    final StackgresClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update cluster's restore config");

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());

  }

  private StackgresClusterReview getCreationReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackgresClusterReview.class);
  }
  private StackgresClusterReview getUpdateReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/restore_config_update.json",
            StackgresClusterReview.class);
  }

}