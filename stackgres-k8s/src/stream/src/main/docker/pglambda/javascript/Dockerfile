ARG BASE_IMAGE

FROM scratch AS copy
  # WORKDIR is what creates /opt/app-root/src in this stage, the COPY below can
  # not create it on its own
  WORKDIR /opt/app-root/src/
  COPY 'src/main/docker/LICENCE' '/licenses/stackgres'
  COPY 'src/main/docker/pglambda/javascript/index.js' /opt/app-root/src/.
  COPY 'src/main/docker/pglambda/javascript/package.json' /opt/app-root/src/.
  COPY 'src/main/docker/pglambda/javascript/package-lock.json.out' /opt/app-root/src/package-lock.json

FROM "$BASE_IMAGE"
  # This copy overwrites /opt/app-root/src of the base image, that npm needs to
  # write to as the user 1001 of the base image. Set the owner explicitly: only
  # BuildKit gives a directory created by WORKDIR to the current USER, buildah
  # and the classic docker builder create it owned by root.
  COPY --from=copy --chown=1001:0 / /

  RUN npm install && \
    npm ci --only=production

  ENV PORT 8080
  EXPOSE $PORT

  CMD npm run production

  ARG STACKGRES_VERSION BUILD_HASH
  LABEL \
    name=stackgres/pglambda \
    maintainer="OnGres Team <stackgres@ongres.com>" \
    vendor="OnGres Inc." \
    version="$STACKGRES_VERSION" \
    release="$BUILD_HASH" \
    summary="StackGres PgLambda JavaScript" \
    description="StackGres PgLambda JavaScript"

