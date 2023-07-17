/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SgProfileDefaultValuesMutator
    extends AbstractValuesMutator<StackGresProfile, SgProfileReview>
    implements ProfileMutator {

  @Inject
  public SgProfileDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresProfile> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  protected Class<StackGresProfile> getResourceClass() {
    return StackGresProfile.class;
  }

}
