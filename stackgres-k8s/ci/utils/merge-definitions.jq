. as $in | [paths | select(.[0] == "definitions" and (. | length) == 2)] as $definitions
  | ($definitions | length | tostring) as $definition_count
  | reduce $definitions[] as $definition ({in: $in, index: 0};
    . as $accumulator
      | (if $debug then "[" + ($accumulator.index | tostring) + "/" + $definition_count + "]" | debug else . end)
      | (if $debug then "Expanded Definition: " + ($definition | tostring) | debug else . end)
      | $accumulator.in | [paths(. == "#/definitions/" + $definition[-1]) | select(.[0] == "definitions")] as $refs
      | {in: (reduce $refs[] as $ref ($accumulator.in;
        . as $ref_accumulator | (if $debug then "Expanded $ref: " + ($ref[0:-1] | tostring) | debug else . end) | $ref_accumulator
          | setpath($ref[0:-1]; ($ref_accumulator|getpath($definition)))
        )), index: ($accumulator.index + 1)}
    )
  | .in
