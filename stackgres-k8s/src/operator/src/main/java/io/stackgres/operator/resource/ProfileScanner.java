/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class ProfileScanner
    extends AbstractKubernetesCustomResourceScanner<StackGresProfile, StackGresProfileList> {

  private final KubernetesClient client;

  @Inject
  public ProfileScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  protected Tuple5<KubernetesClient, String,
      Class<StackGresProfile>,
      Class<StackGresProfileList>,
      Class<? extends Doneable<StackGresProfile>>> arguments() {

    return Tuple.tuple(client, StackGresProfileDefinition.NAME,
        StackGresProfile.class, StackGresProfileList.class,
        StackGresProfileDoneable.class);

  }
}
