/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.inject.Singleton;

import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;

@Singleton
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultProfileConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresProfile, SgProfileReview>
    implements SgProfileValidator {

}
