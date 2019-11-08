/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class ProfileConfigFinder extends AbstractKubernetesCustomResourceFinder<StackGresProfile> {

  private final KubernetesClientFactory kubernetesClientFactory;

  /**
   * Create a {@code ProfileConfigFinder} instance.
   */
  @Inject
  public ProfileConfigFinder(KubernetesClientFactory kubernetesClientFactory) {
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  @Override
  protected Tuple5<KubernetesClientFactory, String, Class<StackGresProfile>,
      Class<? extends KubernetesResourceList<StackGresProfile>>,
          Class<? extends Doneable<StackGresProfile>>> arguments() {
    return Tuple.tuple(kubernetesClientFactory, StackGresProfileDefinition.NAME,
        StackGresProfile.class, StackGresProfileList.class,
        StackGresProfileDoneable.class);
  }

}
