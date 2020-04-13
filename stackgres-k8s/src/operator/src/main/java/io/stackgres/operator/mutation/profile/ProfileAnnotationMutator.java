/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class ProfileAnnotationMutator
    extends AbstractAnnotationMutator<StackGresProfile, SgProfileReview>
    implements ProfileMutator {
}
