/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.crd.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class ProfileConfigFinder extends AbstractCustomResourceFinder<StackGresProfile> {

  /**
   * Create a {@code ProfileConfigFinder} instance.
   */
  @Inject
  public ProfileConfigFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresProfileDefinition.NAME,
        StackGresProfile.class, StackGresProfileList.class,
        StackGresProfileDoneable.class);
  }

  public ProfileConfigFinder() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
