/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileInitializerTest extends AbstractInitializerTest<StackGresProfile> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresProfile> getInstance(
      CustomResourceFinder<StackGresProfile> resourceFinder,
      CustomResourceScheduler<StackGresProfile> resourceScheduler,
      DefaultCustomResourceFactory<StackGresProfile> resourceFactory, InitializationQueue queue) {
    return new ProfileInitializer(resourceFinder, resourceScheduler, resourceFactory, queue);
  }

  @Override
  StackGresProfile getDefaultCR() {
    return JsonUtil
        .readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
  }
}