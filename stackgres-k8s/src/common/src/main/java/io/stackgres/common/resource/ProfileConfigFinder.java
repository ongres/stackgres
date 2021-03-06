/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;

@ApplicationScoped
public class ProfileConfigFinder extends AbstractCustomResourceFinder<StackGresProfile> {

  /**
   * Create a {@code ProfileConfigFinder} instance.
   */
  @Inject
  public ProfileConfigFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresProfile.class, StackGresProfileList.class);
  }

  public ProfileConfigFinder() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
