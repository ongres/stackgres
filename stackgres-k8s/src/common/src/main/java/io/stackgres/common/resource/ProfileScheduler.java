/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;

@ApplicationScoped
public class ProfileScheduler
    extends AbstractCustomResourceScheduler<StackGresProfile, StackGresProfileList> {

  public ProfileScheduler() {
    super(StackGresProfile.class, StackGresProfileList.class);
  }

}
