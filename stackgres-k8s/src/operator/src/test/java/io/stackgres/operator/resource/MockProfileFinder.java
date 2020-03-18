/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.test.Mock;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.utils.JsonUtil;


public class MockProfileFinder implements CustomResourceFinder<StackGresProfile> {

  @Override
  public Optional<StackGresProfile> findByNameAndNamespace(String name, String namespace) {
    return Optional.of(JsonUtil
        .readFromJson("stackgres_profiles/size-xs.json",  StackGresProfile.class));
  }
}
