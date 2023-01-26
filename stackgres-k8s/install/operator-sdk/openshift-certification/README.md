# Generate the token

```
printf %s:%s stackgresbot "$DOCKER_API_TOKEN" | base64 | jq -R '{auths:{"docker.io":{auth: .}}}'
```

# Cleanup preflight

```
rm -rf artifacts/ preflight.log
```

# Run preflight

```
preflight check container \
        --certification-project-id=636295f5acb1c406b78815ec \
        --submit \
        --docker-config ./temp-auth.json \
        --pyxis-api-token "$PYXIS_API_TOKEN" \
        docker.io/stackgres/restapi:main-jvm
```
