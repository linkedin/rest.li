#!/usr/bin/env bash

# Ensure that this is being run by Travis
if [ "$TRAVIS" != "true" ] || [ "$USER" != "travis" ]; then
  echo "This script should only be run by Travis CI."
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

# If the project version is being bumped in this PR, assert that the changelog contains an entry for it
if (! $RELEASE_CANDIDATE) &&
    (git diff ${TRAVIS_BRANCH}...HEAD -- gradle.properties | grep -F "+version=$VERSION" > /dev/null) &&
    ! ( (cat CHANGELOG.md | grep -F "## [$VERSION] -" > /dev/null) &&
        (cat CHANGELOG.md | grep -F "[$VERSION]: https" > /dev/null) ); then
  echo "This change bumps the project version to $VERSION, but no changelog entry could be found for this version!"
  echo 'Please update CHANGELOG.md using the changelog helper script.'
  echo 'For more info, run: ./scripts/update-changelog --help'
  exit 1
fi

# Output something every 9 minutes, otherwise Travis will abort after 10 minutes of no output
while sleep 9m; do echo "[Ping] Keeping Travis job alive ($((SECONDS / 60)) minutes)"; done &
WAITER_PID=$!

# Skip tests if building a tag to prevent flaky releases
if [ ! -z "$TRAVIS_TAG" ]; then
  EXTRA_ARGS='-x test'
else
  EXTRA_ARGS=''
fi

# Run the actual build
./gradlew build $EXTRA_ARGS
EXIT_CODE=$?

# Kill the waiter job
kill $WAITER_PID

if [ $EXIT_CODE != 0 ]; then
  exit 1
fi
