/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ApplicationException;

@Provider
public class ApplicationExceptionMapper extends AbstractGenericExceptionMapper<ApplicationException>
    implements ExceptionMapper<ApplicationException> {

}
