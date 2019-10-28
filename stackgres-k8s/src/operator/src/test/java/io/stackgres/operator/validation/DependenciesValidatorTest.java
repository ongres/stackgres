/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Optional;

import io.stackgres.operator.common.KubernetesScanner;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.PgConfigReview;
import io.stackgres.operator.validation.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class DependenciesValidatorTest {

  private DependenciesValidator validator;

  @Mock
  private KubernetesScanner<StackGresClusterList> clusterScanner;


  @BeforeEach
  void setUp() {

    validator = new DependenciesValidator(clusterScanner);

  }

  @Test
  void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);

    validator.validate(review);

    verify(clusterScanner, never()).findResources();
    verify(clusterScanner, never()).findResources(anyString());

  }

  @Test
  void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

    PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);

    validator.validate(review);

    verify(clusterScanner, never()).findResources();
    verify(clusterScanner, never()).findResources(anyString());

  }

  @Test
  void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() throws ValidationFailed {

    PgConfigReview review = JsonUtil
        .readFromJson("pgconfig_allow_request/pgconfig_delete.json",
            PgConfigReview.class);

    StackGresClusterList clusterList = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class);

    when(clusterScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.of(clusterList));


    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    assertEquals("Can't delete sppgconfig " + review.getRequest().getName() + " because " +
        "the spcluster " + clusterList.getItems().get(0).getMetadata().getName()
        + " dependes on it", ex.getResult().getMessage());

  }

  @Test
  void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

    PgConfigReview review = JsonUtil
        .readFromJson("pgconfig_allow_request/pgconfig_delete.json",
            PgConfigReview.class);

    when(clusterScanner.findResources(review.getRequest().getNamespace()))
        .thenReturn(Optional.empty());

    validator.validate(review);

    verify(clusterScanner, never()).findResources();
    verify(clusterScanner).findResources(review.getRequest().getNamespace());

  }
}
