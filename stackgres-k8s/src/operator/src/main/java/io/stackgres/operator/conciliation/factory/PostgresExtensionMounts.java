/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.jooq.lambda.Seq;

//TODO extract format
@ApplicationScoped
public class PostgresExtensionMounts implements VolumeMountsProvider<PostgresContainerContext> {

  static final String PG_BIN_PATH_FORMAT = "/usr/lib/postgresql/%s/bin";
  static final String PG_LIB64_PATH_FORMAT = "/usr/lib64";
  static final String PG_EXTRA_LIB_PATH_FORMAT = "/usr/lib/postgresql/%s/extra/lib";

  @Inject
  PostgresDataMounts postgresData;

  @Inject
  ContainerUserOverrideMounts containerUserOverride;

  @Override
  public List<VolumeMount> getVolumeMounts(PostgresContainerContext context) {

    String pgVersion = context.getPostgresVersion();
    String majorVersion = context.getPostgresMajorVersion();
    String buildMajorVersion = context.getImageBuildMajorVersion();

    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresData.getVolumeMounts(context))
        .addAll(containerUserOverride.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(PG_LIB64_PATH_FORMAT)
                .withSubPath(String.format("relocated/%s/usr/lib64", pgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format(PG_BIN_PATH_FORMAT, pgVersion))
                .withSubPath(
                    String.format("relocated/%s/usr/lib/postgresql/%s/bin", pgVersion, pgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/lib/postgresql/%s/lib", pgVersion))
                .withSubPath(
                    String.format("relocated/%s/usr/lib/postgresql/%s/lib", pgVersion, pgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/share/postgresql/%s", pgVersion))
                .withSubPath(
                    String.format("relocated/%s/usr/share/postgresql/%s", pgVersion, pgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/share/postgresql/%s/extension", pgVersion))
                .withSubPath(
                    String.format("extensions/%s/%s/usr/share/postgresql/%s/extension",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/lib/postgresql/%s/extra/bin", pgVersion))
                .withSubPath(
                    String.format("extensions/%s/%s/usr/lib/postgresql/%s/bin",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/lib/postgresql/%s/extra/lib", pgVersion))
                .withSubPath(
                    String.format("extensions/%s/%s/usr/lib64",
                        majorVersion,
                        buildMajorVersion))
                .build()
        )
        .addAll(context.getInstalledExtensions()
            .stream()
            .flatMap(ie -> Optional.ofNullable(ie.getExtraMounts())
                .orElse(List.of())
                .stream())
            .distinct()
            .map(extraMount -> new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(extraMount)
                .withSubPath(String.format("extensions/%s/%s%s",
                    majorVersion,
                    buildMajorVersion,
                    extraMount))
                .build())
            .collect(Collectors.toUnmodifiableList()))
        .build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(PostgresContainerContext context) {

    String postgresVersion = context.getPostgresVersion();
    String majorVersion = context.getPostgresMajorVersion();
    String buildMajorVersion = context.getImageBuildMajorVersion();

    return ImmutableList.<EnvVar>builder()
        .addAll(postgresData.getDerivedEnvVars(context))
        .addAll(containerUserOverride.getDerivedEnvVars(context))
        .add(
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_BASE_PATH")
                .withValue("/var/lib/postgresql/extensions")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s",
                        majorVersion,
                        buildMajorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_BINARIES_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s/usr/lib/postgresql/%s",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_BIN_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s/usr/lib/postgresql/%s/bin",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_LIB_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s/usr/lib/postgresql/%s/lib",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_SHARE_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s/usr/share/postgresql/%s",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_EXTENSION_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s/usr/share/postgresql/%s/extension",
                        majorVersion,
                        buildMajorVersion,
                        majorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSIONS_LIB64_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/extensions/%s/%s/usr/lib64",
                        majorVersion,
                        buildMajorVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_LIB64_PATH")
                .withValue("/usr/lib64")
                .build(),

            new EnvVarBuilder()
                .withName("PG_BINARIES_PATH")
                .withValue(
                    String.format(
                        "/usr/lib/postgresql/%s",
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_BIN_PATH")
                .withValue(
                    String.format(PG_BIN_PATH_FORMAT, postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_LIB_PATH")
                .withValue(
                    String.format(
                        "/usr/lib/postgresql/%s/lib",
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTRA_BIN_PATH")
                .withValue(
                    String.format("/usr/lib/postgresql/%s/extra/bin", postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTRA_LIB_PATH")
                .withValue(
                    String.format("/usr/lib/postgresql/%s/extra/lib", postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_SHARE_PATH")
                .withValue(
                    String.format(
                        "/usr/share/postgresql/%s",
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXTENSION_PATH")
                .withValue(
                    String.format(
                        "/usr/share/postgresql/%s/extension",
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/relocated/%s",
                        postgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_LIB64_PATH")
                .withValue(String.format(
                    "/var/lib/postgresql/relocated/%s/usr/lib64",
                    postgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_BINARIES_PATH")
                .withValue(
                    String.format("/var/lib/postgresql/relocated/%s/usr/lib/postgresql/%s",
                        postgresVersion,
                        postgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_BIN_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/relocated/%s/usr/lib/postgresql/%s/bin",
                        postgresVersion,
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_LIB_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/relocated/%s/usr/lib/postgresql/%s/lib",
                        postgresVersion,
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_SHARE_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/relocated/%s/usr/share/postgresql/%s",
                        postgresVersion,
                        postgresVersion)
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_RELOCATED_EXTENSION_PATH")
                .withValue(
                    String.format(
                        "/var/lib/postgresql/relocated/%s/usr/share/postgresql/%s/extension",
                        postgresVersion,
                        postgresVersion
                    )
                )
                .build(),
            new EnvVarBuilder()
                .withName("PG_UPGRADE_PATH")
                .withValue("/var/lib/postgresql/upgrade")
                .build(),
            new EnvVarBuilder()
                .withName("PATH")
                .withValue(Seq.of(
                    String.format("/usr/lib/postgresql/%s/extra/bin", postgresVersion),
                    String.format(PG_BIN_PATH_FORMAT, postgresVersion),
                    "/usr/local/sbin",
                    "/usr/local/bin",
                    "/usr/sbin",
                    "/usr/bin",
                    "/sbin",
                    "/bin")
                    .toString(":"))
                .build(),
            new EnvVarBuilder()
                .withName("LD_LIBRARY_PATH")
                .withValue(String.format(PG_EXTRA_LIB_PATH_FORMAT, postgresVersion))
                .build()
        )
        .build();
  }
}

