/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

@ApplicationScoped
public class ProfileInitializer extends
    AbstractDefaultCustomResourceInitializer<StackGresProfile> {

  @Inject
  public ProfileInitializer(CustomResourceFinder<StackGresProfile> resourceFinder,
      CustomResourceScheduler<StackGresProfile> resourceScheduler,
      DefaultCustomResourceFactory<StackGresProfile> resourceFactory) {
    super(resourceFinder, resourceScheduler, resourceFactory);
  }

  public ProfileInitializer() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
