/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper extends AbstractGenericExceptionMapper<Throwable> {

}
