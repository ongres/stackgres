ARG BASE_IMAGE

FROM "$BASE_IMAGE"
  COPY 'src/main/docker/LICENCE' '/licenses/stackgres'

  COPY 'src/main/docker/pglambda/javascript/package.json' ./
  COPY 'src/main/docker/pglambda/javascript/package-lock.json' ./
  RUN npm install
  RUN npm ci --only=production
  
  ENV PORT 8080
  EXPOSE $PORT

  CMD npm run production
  
  COPY 'src/main/docker/pglambda/javascript/index.js' ./

