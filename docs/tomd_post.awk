# awk post-processor - after pandoc
# re-creates !include sections extracted by tomd_pre.awk

/^!include tomd-include-/ {
  while ((getline line < $2) > 0) { print line }
  close($2);
  next;
}

{ print }
