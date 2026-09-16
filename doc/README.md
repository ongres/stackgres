## Building

Dependencies:

- Java 21
- python pip yq
- [crdoc](https://github.com/fybrik/crdoc)
- [helm-docs](https://github.com/norwoodj/helm-docs)

You can build the doc in the following way:

```bash
# pre-requisites
cd stackgres-k8s/src
./mvnw clean package -DskipTests
sh restapi/src/main/swagger/build.sh
cd -

doc/build.sh
docker run -v "$(pwd)/doc:/src" -p 8313:8313 klakegg/hugo:0.81.0-ubuntu server --bind 0.0.0.0 --port 8313
```

This will copy the required CRD YAML files to the doc sources and start a Hugo server to preview the docs.
Then you can go to http://localhost:8313

## Updating Swagger UI

The REST API reference page renders `sg-swagger.yaml` with a vendored copy of
[Swagger UI](https://github.com/swagger-api/swagger-ui), currently **5.32.11**.
The version has to be new enough for the OpenAPI version declared by the
generated spec (OpenAPI 3.1 requires Swagger UI 5 or later).

To update it, replace the three vendored files with the ones from the matching
`swagger-ui-dist` release:

```bash
SWAGGER_UI_VERSION=5.32.11
for FILE in js/swagger-ui-bundle.js js/swagger-ui-standalone-preset.js css/swagger-ui.css
do
  curl -f -o "doc/themes/sg-doc/static/$FILE" \
    "https://cdn.jsdelivr.net/npm/swagger-ui-dist@$SWAGGER_UI_VERSION/${FILE#*/}"
done
```
