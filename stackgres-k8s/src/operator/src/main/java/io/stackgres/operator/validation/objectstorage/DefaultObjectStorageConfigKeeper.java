/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultObjectStorageConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresObjectStorage, StackGresObjectStorageReview>
    implements ObjectStorageValidator {

}
