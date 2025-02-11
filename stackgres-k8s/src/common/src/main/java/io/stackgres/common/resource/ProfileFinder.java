/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProfileFinder extends AbstractCustomResourceFinder<StackGresProfile> {

  /**
   * Create a {@code ProfileConfigFinder} instance.
   */
  @Inject
  public ProfileFinder(KubernetesClient client) {
    super(client, StackGresProfile.class, StackGresProfileList.class);
  }

}
