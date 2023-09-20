#!/usr/bin/env bash

# Ensure that this is being run in CI by GitHub Actions
if [ "$CI" != "true" ] || [ "$GITHUB_ACTIONS" != "true" ]; then
  echo "This script should only be run in CI by GitHub Actions."
  exit 2
fi

# Ensure that the script is being run from the root project directory
PROPERTIES_FILE='gradle.properties'
if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Could not find $PROPERTIES_FILE, are you sure this is being run from the root project directory?"
  echo "PWD: ${PWD}"
  exit 1
fi

# Determine the current version
VERSION=$(awk 'BEGIN { FS = "=" }; $1 == "version" { print $2 }' $PROPERTIES_FILE | awk '{ print $1 }')
if [ -z "$VERSION" ]; then
  echo "Could not read the version from $PROPERTIES_FILE, please fix it and try again."
  exit 1
fi

# Determine if the version is a release candidate version
RELEASE_CANDIDATE=false
if [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+-rc\.[0-9]+$ ]]; then
  RELEASE_CANDIDATE=true
fi

# TODO: Is this needed on GitHub Actions? Travis aborts after 10 minutes of no output, not sure about GA
# while sleep 9m; do echo "[Ping] Keeping Travis job alive ($((SECONDS / 60)) minutes)"; done &
# WAITER_PID=$!

# For PR builds only...
if [ ! -z "$GITHUB_HEAD_REF" ] && [ ! -z "$GITHUB_BASE_REF" ]; then
  # Fetch the PR base ref so it can be used to compute diffs
  git fetch origin ${GITHUB_BASE_REF}:${GITHUB_BASE_REF}
  # If the project version is being bumped in this PR, assert that the changelog contains an entry for it
  if (! $RELEASE_CANDIDATE) &&
      (git diff ${GITHUB_BASE_REF}...HEAD -- gradle.properties | grep -F "+version=$VERSION" > /dev/null) &&
      ! ( (cat CHANGELOG.md | grep -F "## [$VERSION] -" > /dev/null) &&
          (cat CHANGELOG.md | grep -F "[$VERSION]: https" > /dev/null) ); then
    echo "This change bumps the project version to $VERSION, but no changelog entry could be found for this version!"
    echo 'Please update CHANGELOG.md using the changelog helper script.'
    echo 'For more info, run: ./scripts/update-changelog --help'
    exit 1
  fi
  # Skip module-specific tests if its module dependencies haven't been touched
  CONDITIONAL_TESTING_MODULES='d2 r2-int-test restli-int-test'
  echo "This is a PR build, so testing will be conditional for these subprojects: [${CONDITIONAL_TESTING_MODULES// /,}]"
  # If any Gradle file was touched, run all tests just to be safe
  if (git diff ${GITHUB_BASE_REF}...HEAD --name-only | grep '\.gradle' > /dev/null); then
    echo "This PR touches a file matching *.gradle, so tests will be run for all subprojects."
  else
    # Have to prime the comma-separated list with a dummy value because list construction in bash is hard...
    EXTRA_ARGS="${EXTRA_ARGS} -Ppegasus.skipTestsForSubprojects=primer"
    # For all the following modules (which have lengthy tests), determine if they can be skipped
    for MODULE in $CONDITIONAL_TESTING_MODULES; do
      echo "Checking test dependencies for subproject $MODULE..."
      MODULE_DEPENDENCIES="$(./scripts/get-module-dependencies $MODULE testRuntimeClasspath | tr '\n' ' ')"
      # Create regex to capture lines in the diff's paths, e.g. 'a b c' -> '^\(a\|b\|c\)/'
      PATH_MATCHING_REGEX="^\\($(echo $MODULE_DEPENDENCIES | sed -z 's/ \+/\\|/g;s/\\|$/\n/g')\\)/"
      if [ ! -z "$PATH_MATCHING_REGEX" ] && ! (git diff ${GITHUB_BASE_REF}...HEAD --name-only | grep "$PATH_MATCHING_REGEX" > /dev/null); then
        echo "Computed as... [${MODULE_DEPENDENCIES// /,}]"
        echo "None of $MODULE's module dependencies have been touched, skipping tests for $MODULE."
        EXTRA_ARGS="${EXTRA_ARGS},$MODULE"
      else
        echo "Some of $MODULE's module dependencies have been touched, tests for $MODULE will remain enabled."
      fi
    done
  fi
fi

# Run the actual build
./gradlew build $EXTRA_ARGS
EXIT_CODE=$?

# TODO: Figure out if this can be removed as well for GitHub Actions
# Kill the waiter job
# kill $WAITER_PID

if [ $EXIT_CODE != 0 ]; then
  exit 1
fi
