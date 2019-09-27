/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.validators;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.services.PostgresConfigFinder;
import io.stackgres.operator.validation.AdmissionReview;

@ApplicationScoped
public class PostgresVersion implements Validator {

  private PostgresConfigFinder configFinder;

  @Inject
  public PostgresVersion(PostgresConfigFinder configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

  }

}
