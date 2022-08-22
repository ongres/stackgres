/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import java.util.HashMap;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.testutil.fixture.Fixture;

public class VersionedFixture<T extends HasMetadata> extends Fixture<T> {

  @Override
  protected void transform(T resource) {
    if (resource.getMetadata().getAnnotations() == null) {
      resource.getMetadata().setAnnotations(new HashMap<>());
    }
    resource.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
  }

}
