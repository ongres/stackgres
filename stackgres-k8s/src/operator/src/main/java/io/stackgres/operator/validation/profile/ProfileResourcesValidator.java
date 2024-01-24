/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ProfileResourcesValidator implements SgProfileValidator {

  @Override
  public void validate(SgProfileReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final StackGresProfile profile = review.getRequest().getObject();

      checkQuantity(profile.getSpec().getCpu(),
          "spec.cpu");
      checkQuantity(profile.getSpec().getMemory(),
          "spec.memory");
      checkQuantity(Optional.ofNullable(profile.getSpec().getHugePages())
          .map(StackGresProfileHugePages::getHugepages2Mi)
          .orElse(null),
          "spec.hugePages.hugepages-2Mi");
      checkQuantity(Optional.ofNullable(profile.getSpec().getHugePages())
          .map(StackGresProfileHugePages::getHugepages1Gi)
          .orElse(null),
          "spec.hugePages.hugepages-1Gi");
      for (var container : Optional
          .ofNullable(profile.getSpec().getContainers())
          .stream()
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .toList()) {
        checkQuantity(container.getValue().getCpu(),
            "spec.containers." + container.getKey() + ".cpu");
        checkQuantity(container.getValue().getMemory(),
            "spec.containers." + container.getKey() + ".memory");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .orElse(null),
            "spec.containers." + container.getKey() + ".hugePages.hugepages-2Mi");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .orElse(null),
            "spec.containers." + container.getKey() + ".hugePages.hugepages-1Gi");
      }
      for (var container : Optional
          .ofNullable(profile.getSpec().getInitContainers())
          .stream()
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .toList()) {
        checkQuantity(container.getValue().getCpu(),
            "spec.initContainers." + container.getKey() + ".cpu");
        checkQuantity(container.getValue().getMemory(),
            "spec.initContainers." + container.getKey() + ".memory");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .orElse(null),
            "spec.initContainers." + container.getKey() + ".hugePages.hugepages-2Mi");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .orElse(null),
            "spec.initContainers." + container.getKey() + ".hugePages.hugepages-1Gi");
      }
      checkQuantity(Optional.ofNullable(profile.getSpec().getRequests())
          .map(StackGresProfileRequests::getCpu)
          .orElse(null),
          "spec.requests.cpu");
      checkQuantity(Optional.ofNullable(profile.getSpec().getRequests())
          .map(StackGresProfileRequests::getMemory)
          .orElse(null),
          "spec.requests.memory");
      for (var container : Optional
          .ofNullable(profile.getSpec().getRequests())
          .map(StackGresProfileRequests::getContainers)
          .stream()
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .toList()) {
        checkQuantity(container.getValue().getCpu(),
            "spec.requests.containers." + container.getKey() + ".cpu");
        checkQuantity(container.getValue().getMemory(),
            "spec.requests.containers." + container.getKey() + ".memory");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .orElse(null),
            "spec.requests.containers." + container.getKey() + ".hugePages.hugepages-2Mi");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .orElse(null),
            "spec.requests.containers." + container.getKey() + ".hugePages.hugepages-1Gi");
      }
      for (var container : Optional
          .ofNullable(profile.getSpec().getRequests())
          .map(StackGresProfileRequests::getInitContainers)
          .stream()
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .toList()) {
        checkQuantity(container.getValue().getCpu(),
            "spec.requests.initContainers." + container.getKey() + ".cpu");
        checkQuantity(container.getValue().getMemory(),
            "spec.requests.initContainers." + container.getKey() + ".memory");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .orElse(null),
            "spec.requests.initContainers." + container.getKey() + ".hugePages.hugepages-2Mi");
        checkQuantity(Optional.ofNullable(container.getValue().getHugePages())
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .orElse(null),
            "spec.requests.initContainers." + container.getKey() + ".hugePages.hugepages-1Gi");
      }
    }
  }

  private void checkQuantity(String value, String field) throws ValidationFailed {
    if (Optional.ofNullable(value)
        .map(Quantity::parse)
        .map(Quantity::getNumericalAmount)
        .map(BigDecimal::signum)
        .map(signum -> signum == -1)
        .orElse(false)) {
      failWithMessageAndFields(
          HasMetadata.getKind(StackGresProfile.class),
          ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION),
          "Quantity can not be negative, but was " + value,
          field);
    }
  }
}
