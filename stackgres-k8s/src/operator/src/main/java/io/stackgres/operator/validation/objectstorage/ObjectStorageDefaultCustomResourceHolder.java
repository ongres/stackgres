/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.validation.AbstractDefaultCustomResourceHolder;

@ApplicationScoped
public class ObjectStorageDefaultCustomResourceHolder
    extends AbstractDefaultCustomResourceHolder<StackGresObjectStorage> {

}
