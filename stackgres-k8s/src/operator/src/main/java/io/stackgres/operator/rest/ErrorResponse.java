/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ErrorResponse {
  private String type;
  private String message;
  private String documentationLink;

  public String getType() {
    return type;
  }

  public ErrorResponse setType(String type) {
    this.type = type;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public ErrorResponse setMessage(String message) {
    this.message = message;
    return this;
  }

  public String getDocumentationLink() {
    return documentationLink;
  }

  public ErrorResponse setDocumentationLink(String documentationLink) {
    this.documentationLink = documentationLink;
    return this;
  }

  public static ErrorResponse create(Throwable throwable) {
    return new ErrorResponse().setType(throwable.getClass().getName())
        .setMessage(throwable.getMessage());
  }
}
