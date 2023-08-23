. as $source
| [ .[0].spec.versions[0].schema.openAPIV3Schema.properties[$ROOT_KEY].properties
    | . as $root
    | paths
    | . as $path
    | ($root | getpath($path)) as $field
    | select($field | (type == "object" and has("type"))) 
    | del(.[] | select(. == "items" or . == "properties" or . == "additionalProperties"))
    | map(sub("\\.";"\\."))
    | join(".")
    | . as $section_path
    | select((["boolean", "string", "integer"]
        | any(. == $field.type)) or ($source[1][$ROOT_KEY + "Descriptors"] | has($section_path)))
    | select($source[1][$ROOT_KEY + "Ignore"]
        | if . != null then . else [] end
        | any(. as $ignore_path | $section_path | startswith($ignore_path + "."))
        | not)
    | $root
    | getpath($path)
    | {
        path: $section_path,
        description: .description,
        displayName: ($section_path | sub("\\\\"; "") | split(".") | join(" ")
            | [ splits("(?=[A-Z ])") ]
            | map(sub(" ";""))
            | map(select(. != ""))
            | map(split("")
                | .[0] |= ascii_upcase
                | join(""))
            | reduce .[] as $word (
                {};
                if .previous == null
                then .join = $word
                else (
                    if ((.previous | length) > 1 or ($word | length) > 1)
                    then .join += " "
                    else .
                    end
                    | .join += $word
                    )
                end
                | .previous = $word)
            | .join
            | sub("Sg Cluster"; "SGCluster")
            | sub("Sg Instance Profile"; "SGInstanceProfile")
            | sub("Sg Postgres Config"; "SGPostgresConfig")
            | sub("Sg Pooling Config"; "SGPoolingConfig")
            | sub("Sg Object Storage"; "SGObjectStorage")
            | sub("Sg Script"; "SGScript")
            | sub("Sg Backup"; "SGBackup")
            | sub("Sg Distributed Logs"; "SGDistributedLogs")
            | sub("Sg Config"; "SGConfig")
            | sub("Sg Sharded Cluster"; "SGShardedCluster")
            ),
        "x-descriptors": (
            if "boolean" == ($field.type)
            then
              [ "urn:alm:descriptor:com.tectonic.ui:booleanSwitch" ]
            else
              null
            end
        )
      }
    | {
      key: .path,
      value: .
      }
  ]
| from_entries
| . * ($source[1][$ROOT_KEY + "Descriptors"]
    | if . != null
      then .
      else {}
      end)
| to_entries
| map(.value)
| { ($ROOT_KEY + "Descriptors"): . }
