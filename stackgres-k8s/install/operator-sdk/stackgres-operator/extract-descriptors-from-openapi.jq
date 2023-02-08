. as $source
  | [ .[0].spec.versions[0].schema.openAPIV3Schema.properties[$ROOT_KEY].properties
      | . as $root
      | paths
      | . as $path
      | select(["object", "array", "boolean", "string", "integer"]
          | any(. == ($root | getpath($path)
              | select(type == "object")
              | .type)))
      | del(.[] | select(. == "items" or . == "properties"))
      | map(sub("\\.";"\\."))
      | join(".")
      | . as $section_path
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
              | map(if . == "Sg" then "StackGres" else . end)
              | join(" ")),
          "x-descriptors": (
              if "boolean" == ($root | getpath($path) | .type)
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
  | . * ($source[1]
      | if . != null
        then .[$ROOT_KEY + "Descriptors"]
            | if . != null
              then .
              else {}
              end
        else {}
        end)
  | to_entries
  | map(.value)
  | { ($ROOT_KEY + "Descriptors"): . }
