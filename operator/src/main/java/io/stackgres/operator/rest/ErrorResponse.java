package io.stackgres.operator.rest;

public class ErrorResponse {
  private String message;
  private String documentationLink;

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

  public static ErrorResponse create(String message) {
    return new ErrorResponse().setMessage(message);
  }
}