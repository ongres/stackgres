/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.validation.AbstractDefaultCustomResourceHolder;

@ApplicationScoped
public class ProfileDefaultCustomResourceHolder
    extends AbstractDefaultCustomResourceHolder<StackGresProfile> {
}
