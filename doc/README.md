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
sh api-web/src/main/swagger/build.sh
cd -

doc/build.sh
docker run -v "$(pwd)/doc:/src" -p 8313:8313 klakegg/hugo:0.81.0-ubuntu server --bind 0.0.0.0 --port 8313
```

This will copy the required CRD YAML files to the doc sources and start a Hugo server to preview the docs.
Then you can go to http://localhost:8313
