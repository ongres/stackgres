/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.spi.ReaderException;

@Provider
public class ReaderExceptionMapper extends AbstractGenericExceptionMapper<ReaderException> {

}
