package io.stackgres.operator.validation;

import io.fabric8.kubernetes.api.model.GroupVersionKind;

public class AdmissionReview extends GroupVersionKind {

  private AdmissionRequest request;

  public AdmissionRequest getRequest() {
    return request;
  }

  public void setRequest(AdmissionRequest request) {
    this.request = request;
  }
}
