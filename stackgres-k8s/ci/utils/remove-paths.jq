. as $in | [paths | select(.|any(.|tostring|startswith("x-")))] as $xpaths
  | ($xpaths | length | tostring) as $xpath_count
  | reduce $xpaths[] as $xpath ({in: $in, index: 0};
    . as $accumulator
      | (if $debug then "[" + ($accumulator.index | tostring) + "/" + $xpath_count + "]" | debug else . end)
      | (if $debug then "Remove extended property: " + ($xpath | tostring) | debug else . end)
      | {in: ($accumulator.in | delpaths([$xpath])), index: ($accumulator.index + 1)}
    )
  | .in
