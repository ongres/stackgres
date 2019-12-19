/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;

@ApplicationScoped
public class DefaultProfileConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresProfile, SgProfileReview>
    implements SgProfileValidator {

}
