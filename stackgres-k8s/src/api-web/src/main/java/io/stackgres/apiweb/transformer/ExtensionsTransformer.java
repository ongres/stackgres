/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Collection;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.extension.Extension;
import io.stackgres.apiweb.dto.extension.ExtensionPublisher;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtension;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.extension.StackGresExtensionPublisher;
import io.stackgres.common.extension.StackGresExtensionVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ExtensionsTransformer {

  private final ExtensionMetadataManager extensionMetadataManager;

  private final ObjectMapper mapper;

  @Inject
  public ExtensionsTransformer(ExtensionMetadataManager extensionMetadataManager,
                               ObjectMapper mapper) {
    super();
    this.extensionMetadataManager = extensionMetadataManager;
    this.mapper = mapper;
  }

  public ExtensionsDto toDto(
      Collection<StackGresExtensionMetadata> extensionMetadataList,
      StackGresCluster cluster) {
    ExtensionsDto transformation = new ExtensionsDto();
    transformation.setExtensions(Seq.seq(extensionMetadataList)
        .grouped(StackGresExtensionMetadata::getExtension)
        .map(Tuple2::v1)
        .map(extension -> getExtension(extension, cluster)).toList());
    transformation.setPublishers(Seq.seq(extensionMetadataList)
        .grouped(StackGresExtensionMetadata::getPublisher)
        .map(Tuple2::v1)
        .map(this::getExtensionPublisher).toList());
    return transformation;
  }

  private Extension getExtension(StackGresExtension source, StackGresCluster cluster) {
    Extension transformation = new Extension();
    transformation.setPublisher(source.getPublisherOrDefault());
    transformation.setName(source.getName());
    transformation.setRepository(source.getRepository());
    transformation.setAbstractDescription(source.getAbstractDescription());
    transformation.setDescription(source.getDescription());
    transformation.setLicense(source.getLicense());
    transformation.setTags(source.getTags());
    transformation.setUrl(source.getUrl());
    transformation.setSource(source.getSource());
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setPublisher(source.getPublisherOrDefault());
    extension.setName(source.getName());
    extension.setRepository(source.getRepository());
    transformation.setVersions(
        Seq.seq(extensionMetadataManager.getExtensionsAnyVersion(cluster, extension, false))
            .map(StackGresExtensionMetadata::getVersion)
            .map(StackGresExtensionVersion::getVersion)
            .grouped(Function.identity())
            .map(Tuple2::v1)
            .toList());
    return transformation;
  }

  private ExtensionPublisher getExtensionPublisher(StackGresExtensionPublisher source) {
    return mapper.convertValue(source, ExtensionPublisher.class);
  }

}
