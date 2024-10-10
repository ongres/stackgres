/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import io.stackgres.apiweb.dto.extension.Extension;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.docir.DocirExtension;
import io.stackgres.common.docir.DocirExtensionMetadata;
import io.stackgres.common.docir.DocirExtensionVersion;
import io.stackgres.common.docir.DocirMetadataManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ExtensionsTransformer {

  private final StackGresContext context;

  private final DocirMetadataManager docirMetadataManager;

  @Inject
  public ExtensionsTransformer(
      StackGresContext context,
      DocirMetadataManager docirMetadataManager) {
    this.context = context;
    this.docirMetadataManager = docirMetadataManager;
  }

  public ExtensionsDto toDto(
      Collection<DocirExtensionMetadata> extensionMetadataList,
      StackGresCluster cluster) {
    ExtensionsDto transformation = new ExtensionsDto();
    transformation.setExtensions(Seq.seq(extensionMetadataList)
        .grouped(Function.<DocirExtensionMetadata>identity()
            .andThen(DocirExtensionMetadata::getExtension)
            .andThen(DocirExtension::getName))
        .map(Tuple2::v2)
        .map(extension -> getExtension(extension.findFirst().get(), cluster)).toList());
    transformation.setPublishers(List.of());
    return transformation;
  }

  private Extension getExtension(DocirExtensionMetadata source, StackGresCluster cluster) {
    Extension transformation = new Extension();
    transformation.setName(source.getExtension().getName());
    transformation.setRepository(source.getExtension().getRepository());
    transformation.setAbstractDescription(source.getExtension().getAbstractDescription());
    transformation.setDescription(source.getExtension().getDescription());
    transformation.setLicense(source.getExtension().getLicense());
    transformation.setTags(List.of());
    transformation.setUrl(source.getExtension().getUrl());
    transformation.setSource(source.getExtension().getSource());
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(source.getExtension().getName());
    extension.setRepository(source.getExtension().getRepository());
    transformation.setVersions(
        Seq.seq(docirMetadataManager.getExtensionsAnyVersion(context, cluster, extension, false))
            .grouped(Function.<DocirExtensionMetadata>identity()
                .andThen(DocirExtensionMetadata::getVersion)
                .andThen(DocirExtensionVersion::getVersion))
            .map(Tuple2::v1)
            .sorted(Comparator.comparing(Function.<String>identity()
                .andThen(StackGresUtil::sortableVersion))
                .reversed())
            .toList());
    return transformation;
  }

}
