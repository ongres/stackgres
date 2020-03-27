/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.authentication;

import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.CDI;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.realm.CacheableSecurityRealm;
import org.wildfly.security.auth.server.IdentityCredentials;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.password.spec.ClearPasswordSpec;
import org.wildfly.security.password.spec.PasswordSpec;

public class KubernetesSecretSecurityRealm implements CacheableSecurityRealm {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesSecretSecurityRealm.class);

  private final Supplier<Provider[]> providers;

  KubernetesSecretSecurityRealm(Supplier<Provider[]> providers) {
    this.providers = providers;
  }

  @Override
  public RealmIdentity getRealmIdentity(final Principal principal) {
    if (!(principal instanceof NamePrincipal)) {
      return RealmIdentity.NON_EXISTENT;
    }
    return new KubernetesSecretRealmIdentity(principal.getName());
  }

  @Override
  public SupportLevel getCredentialAcquireSupport(final Class<? extends Credential> credentialType,
      final String algorithmName, final AlgorithmParameterSpec parameterSpec)
      throws RealmUnavailableException {
    return PasswordCredential.class.isAssignableFrom(credentialType)
        ? SupportLevel.POSSIBLY_SUPPORTED : SupportLevel.UNSUPPORTED;
  }

  @Override
  public SupportLevel getEvidenceVerifySupport(final Class<? extends Evidence> evidenceType,
      final String algorithmName) throws RealmUnavailableException {
    Assert.checkNotNullParam("evidenceType", evidenceType);
    return PasswordCredential.canVerifyEvidence(evidenceType, algorithmName)
        ? SupportLevel.SUPPORTED : SupportLevel.UNSUPPORTED;
  }

  @Override
  public void registerIdentityChangeListener(Consumer<Principal> listener) {
    // no notifications from this realm about changes on the underlying storage
  }

  private class KubernetesSecretRealmIdentity implements RealmIdentity {

    private final String name;
    private final String secretName;
    private boolean loaded = false;
    private KubernetesSecretIdentity identity;

    public KubernetesSecretRealmIdentity(String name) {
      this.name = name;
      this.secretName = CDI.current().select(ConfigLoader.class).get()
          .get(ConfigProperty.AUTHENTICATION_SECRET_NAME);
    }

    public Principal getRealmIdentityPrincipal() {
      return new NamePrincipal(name);
    }

    @Override
    public SupportLevel getCredentialAcquireSupport(
        final Class<? extends Credential> credentialType, final String algorithmName,
        final AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
      Assert.checkNotNullParam("credentialType", credentialType);

      KubernetesSecretIdentity identity = getIdentity();
      if (identity != null) {
        return identity.identityCredentials.getCredentialAcquireSupport(credentialType,
            algorithmName, parameterSpec);
      }

      return SupportLevel.UNSUPPORTED;
    }

    @Override
    public <C extends Credential> C getCredential(final Class<C> credentialType)
        throws RealmUnavailableException {
      return getCredential(credentialType, null);
    }

    @Override
    public <C extends Credential> C getCredential(final Class<C> credentialType,
        final String algorithmName) throws RealmUnavailableException {
      return getCredential(credentialType, algorithmName, null);
    }

    @Override
    public <C extends Credential> C getCredential(final Class<C> credentialType,
        final String algorithmName, final AlgorithmParameterSpec parameterSpec)
        throws RealmUnavailableException {
      Assert.checkNotNullParam("credentialType", credentialType);

      KubernetesSecretIdentity identity = getIdentity();
      if (identity != null) {
        return identity.identityCredentials.getCredential(credentialType, algorithmName);
      }

      return null;
    }

    @Override
    public SupportLevel getEvidenceVerifySupport(final Class<? extends Evidence> evidenceType,
        final String algorithmName) throws RealmUnavailableException {
      Assert.checkNotNullParam("evidenceType", evidenceType);

      KubernetesSecretIdentity identity = getIdentity();
      if (identity != null) {
        return identity.identityCredentials.canVerify(evidenceType, algorithmName)
            ? SupportLevel.SUPPORTED
            : SupportLevel.UNSUPPORTED;
      }

      return SupportLevel.UNSUPPORTED;
    }

    @Override
    public boolean verifyEvidence(final Evidence evidence) throws RealmUnavailableException {
      Assert.checkNotNullParam("evidence", evidence);

      KubernetesSecretIdentity identity = getIdentity();
      if (identity != null) {
        return identity.identityCredentials.verify(evidence);
      }

      return false;
    }

    public boolean exists() throws RealmUnavailableException {
      return getIdentity() != null;
    }

    @Override
    public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
      if (!exists()) {
        return AuthorizationIdentity.EMPTY;
      }

      return AuthorizationIdentity.basicIdentity(this.identity.attributes);
    }

    private KubernetesSecretIdentity getIdentity() {
      if (!loaded && this.identity == null) {
        MapAttributes attributes = new MapAttributes();
        attributes.addLast("roles", Roles.ADMIN);
        IdentityCredentials credentials = IdentityCredentials.NONE;

        LOGGER.trace("Extracting password from secret {} at key {}", secretName, name);

        KubernetesClientFactory clientFactory = CDI.current()
            .select(KubernetesClientFactory.class).get();
        try (KubernetesClient client = clientFactory.create()) {
          return Optional.ofNullable(client.secrets().withName(secretName).get())
            .map(Secret::getData)
            .filter(data -> Optional.ofNullable(data.get("user"))
                .map(ResourceUtil::dencodeSecret)
                .map(name::equals)
                .orElse(false))
            .map(data -> data.get("password"))
            .map(ResourceUtil::dencodeSecret)
            .map(Unchecked.function(this::plainPassword))
            .map(password -> new KubernetesSecretIdentity(attributes,
                credentials.withCredential(new PasswordCredential(password))))
            .orElse(null);
        }
      }
      return this.identity;
    }

    private Password plainPassword(String password)
        throws InvalidKeySpecException, NoSuchAlgorithmException {
      final PasswordFactory passwordFactory = PasswordFactory.getInstance(
          ClearPassword.ALGORITHM_CLEAR, providers);
      PasswordSpec passwordSpec = new ClearPasswordSpec(password.toCharArray());
      return passwordFactory.generatePassword(passwordSpec);
    }

    private class KubernetesSecretIdentity {
      private final Attributes attributes;
      private final IdentityCredentials identityCredentials;

      KubernetesSecretIdentity(Attributes attributes, IdentityCredentials identityCredentials) {
        this.attributes = attributes;
        this.identityCredentials = identityCredentials;
      }
    }
  }

}
