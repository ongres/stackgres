/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.exception;

public class ErrorResponseBuilder {
  private String type;
  private String title;
  private String detail;
  private Integer status;
  private String[] fields;

  public ErrorResponseBuilder(String type) {
    this.type = type;
  }

  public ErrorResponseBuilder setTitle(String title) {
    this.title = title;
    return this;
  }

  public ErrorResponseBuilder setDetail(String detail) {
    this.detail = detail;
    return this;
  }

  public ErrorResponseBuilder setStatus(Integer status) {
    this.status = status;
    return this;
  }

  public ErrorResponseBuilder setFields(String[] fields) {
    this.fields = fields != null ? fields.clone() : null;
    return this;
  }

  public ErrorResponse build() {
    return new ErrorResponse(type, title, detail, status, fields);
  }
}
