/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.exception;

import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.spi.WriterException;

@Provider
public class WriterExceptionMapper extends AbstractGenericExceptionMapper<WriterException> {

}
