/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;

@ApplicationScoped
public class DefaultProfileFactory extends AbstractCustomResourceFactory<StackGresProfile> {

  public static final String NAME = "defaultprofile";
  public static final String INSTANCE_PROFILE_DEFAULT_VALUES =
      "instance-profile-default-values.properties";

  @Override
  String getDefaultPropertiesFile() {
    return INSTANCE_PROFILE_DEFAULT_VALUES;
  }

  @Override
  StackGresProfile buildResource(String namespace) {

    StackGresProfile profile = new StackGresProfile();
    profile.getMetadata().setName(generateDefaultName());
    profile.getMetadata().setNamespace(namespace);

    StackGresProfileSpec spec = buildSpec(StackGresProfileSpec.class);

    profile.setSpec(spec);

    return profile;

  }

}
