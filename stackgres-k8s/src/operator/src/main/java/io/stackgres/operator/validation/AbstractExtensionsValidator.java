/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsValidator<T extends AdmissionReview<?>>
    implements Validator<T> {

  protected abstract ClusterExtensionMetadataManager getExtensionMetadataManager();

  @Override
  public void validate(T review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
      case UPDATE: {
        final StackGresCluster cluster = getCluster(review);
        var defaultExtensions = Seq.seq(getDefaultExtensions(cluster))
            .collect(ImmutableList.toImmutableList());
        List<Tuple2<String, Optional<String>>> requiredExtensions =
            Seq.seq(getPostgresExtensions(review))
                .flatMap(List::stream)
                .map(extension -> Tuple.tuple(
                    extension.getName(), Optional.ofNullable(extension.getVersion())))
                .filter(extension -> defaultExtensions.stream()
                    .map(Tuple2::v1).noneMatch(extension.v1::equals))
                .append(defaultExtensions)
                .collect(ImmutableList.toImmutableList());
        List<Tuple2<String, Optional<String>>> toInstallExtensions =
            Seq.seq(getToInstallExtensions(review))
                .flatMap(List::stream)
                .map(extension -> Tuple.tuple(
                    extension.getName(), Optional.ofNullable(extension.getVersion())))
                .collect(ImmutableList.toImmutableList());
        final List<Tuple2<String, Optional<String>>> missingExtensions = Seq.seq(requiredExtensions)
            .filter(requiredExtension -> toInstallExtensions.stream()
                .noneMatch(toInstallExtension -> requiredExtension.v1.equals(
                    toInstallExtension.v1)))
            .sorted(Tuple2::v1)
            .collect(ImmutableList.toImmutableList());
        if (!missingExtensions.isEmpty()) {
          Map<String, List<String>> candidateExtensionVersions = missingExtensions
              .stream()
              .map(missingExtension -> {
                return Tuple.<String, List<String>>tuple(missingExtension.v1,
                    getExtensionMetadataManager().getExtensionsAnyVersion(
                        cluster, Seq.seq(getPostgresExtensions(review))
                            .flatMap(List::stream)
                            .filter(extension -> extension.getName().equals(
                                missingExtension.v1))
                            .findAny()
                            .orElseGet(() -> {
                              StackGresClusterExtension extension = new StackGresClusterExtension();
                              extension.setName(missingExtension.v1);
                              missingExtension.v2.ifPresent(extension::setVersion);
                              return extension;
                            }))
                        .stream()
                        .map(extensionMetadata -> extensionMetadata.getVersion().getVersion())
                        .collect(ImmutableList.toImmutableList()));
              })
              .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
          String errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.EXTENSION_NOT_FOUND);
          String missingExtensionsMessage = Seq.seq(missingExtensions)
              .map(missingExtension -> {
                final Set<String> availableVersions =
                    Set.copyOf(candidateExtensionVersions.get(missingExtension.v1));
                if (!availableVersions.isEmpty()) {
                  return missingExtension.v1
                      + " (available " + Seq.seq(availableVersions).toString(", ") + ")";
                }
                return missingExtension.v1;
              })
              .toString(", ");
          if (missingExtensions.size() == 1) {
            failValidation(errorTypeUri,
                "Extension was not found: " + missingExtensionsMessage);
          } else {
            failValidation(errorTypeUri,
                "Some extensions were not found: " + missingExtensionsMessage);
          }
        }
        break;
      }
      default:
    }
  }

  protected abstract ImmutableList<Tuple2<String, Optional<String>>> getDefaultExtensions(
      StackGresCluster cluster);

  protected abstract StackGresCluster getCluster(T customResource);

  protected abstract Optional<List<StackGresClusterExtension>> getPostgresExtensions(
      T review);

  protected abstract Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      T review);

  protected abstract void failValidation(String reason, String message) throws ValidationFailed;

}
