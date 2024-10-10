/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.util.Map;

import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfileAnnotationMutator
    extends AbstractAnnotationMutator<StackGresInstanceProfile, StackGresInstanceProfileReview>
    implements ProfileMutator {

  private static final long LATEST = StackGresVersion.LATEST.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresInstanceProfile resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (LATEST > version) {
      return Map.of(StackGresKeys.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    }
    return Map.of();
  }

}
