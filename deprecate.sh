grep -o "include '[^']*'" settings.gradle | sed -e "s/^include '/com.linkedin.pegasus:/g" -e "s/'//g" | while read -r module_name ; do
    if [ "$module_name" == "com.linkedin.pegasus:gradle-plugins" ]
    then
        echo "WARNING: $module_name cannot be deprecated due to MPPCX-7165. Skipping deprecation..."
    else
        mint catalog deprecate "$module_name" "$@"
    fi
done
