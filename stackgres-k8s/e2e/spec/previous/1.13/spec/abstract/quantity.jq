def quantity:
  if . == null then 0
  else . as $text
      | [splits("[eEinumkKMGTP]+")][0]
      | . as $first_letter_suffix
      | tonumber as $value
      | $text[($first_letter_suffix|length):($text|length)] as $suffix
      | if $suffix == "" then $value
        elif $suffix == "Ki" then $value * 1024
        elif $suffix == "Mi" then $value * 1024 * 1024
        elif $suffix == "Gi" then $value * 1024 * 1024 * 1024
        elif $suffix == "Ti" then $value * 1024 * 1024 * 1024 * 1024
        elif $suffix == "m" then $value / 1000
        else . end
  end;
