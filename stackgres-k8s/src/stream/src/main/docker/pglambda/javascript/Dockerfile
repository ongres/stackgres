ARG BASE_IMAGE

FROM scratch AS copy
  USER 1001
  WORKDIR /opt/app-root/src/
  USER 0
  COPY 'src/main/docker/LICENCE' '/licenses/stackgres'
  COPY 'src/main/docker/pglambda/javascript/index.js' /opt/app-root/src/.
  COPY 'src/main/docker/pglambda/javascript/package.json' /opt/app-root/src/.
  COPY 'src/main/docker/pglambda/javascript/package-lock.json.out' /opt/app-root/src/package-lock.json

FROM "$BASE_IMAGE"
  COPY --from=copy / /

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

