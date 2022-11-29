/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;

@ApplicationScoped
public class DefaultProfileFactory extends AbstractCustomResourceFactory<StackGresProfile> {

  public static final String NAME = "defaultprofile";
  public static final String INSTANCE_PROFILE_DEFAULT_VALUES =
      "/instance-profile-default-values.properties";

  @Inject
  public DefaultProfileFactory(StackGresPropertyContext<OperatorProperty> context) {
    super(context);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(INSTANCE_PROFILE_DEFAULT_VALUES);
  }

  @Override
  StackGresProfile buildResource(String namespace) {

    StackGresProfile profile = new StackGresProfile();
    profile.getMetadata().setName(generateDefaultName());
    profile.getMetadata().setNamespace(namespace);

    StackGresProfileSpec spec = buildFromDefaults(StackGresProfileSpec.class);

    profile.setSpec(spec);

    return profile;

  }

}
