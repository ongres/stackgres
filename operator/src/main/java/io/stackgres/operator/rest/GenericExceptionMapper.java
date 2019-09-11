package io.stackgres.operator.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
  @Override
  public Response toResponse(Throwable throwable) {
    return Response.serverError().entity(ErrorResponse.create(throwable.getMessage())).build();
  }
}