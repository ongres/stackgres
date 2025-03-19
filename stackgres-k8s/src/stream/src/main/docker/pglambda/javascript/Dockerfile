ARG BASE_IMAGE

FROM "$BASE_IMAGE"
  COPY 'src/main/docker/LICENCE' '/licenses/stackgres'

  COPY 'src/main/docker/pglambda/javascript/package.json' ./
  COPY 'src/main/docker/pglambda/javascript/package-lock.json.out' ./package-lock.json
  RUN npm install
  RUN npm ci --only=production
  
  ENV PORT 8080
  EXPOSE $PORT

  CMD npm run production
  
  COPY 'src/main/docker/pglambda/javascript/index.js' ./

  ARG STACKGRES_VERSION
  ARG BUILD_HASH
  LABEL name=stackgres/pglambda
  LABEL maintainer="OnGres Team <stackgres@ongres.com>"
  LABEL vendor="OnGres Inc."
  LABEL version="$STACKGRES_VERSION"
  LABEL release="$BUILD_HASH"
  LABEL summary="StackGres PgLambda JavaScript"
  LABEL description="StackGres PgLambda JavaScript"

