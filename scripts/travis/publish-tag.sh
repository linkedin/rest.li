#!/usr/bin/env bash

# Ensure that this is being run by Travis
if [ "$TRAVIS" != "true" ] || [ "$USER" != "travis" ]; then
  echo "This script should only be run by Travis CI."
  exit 2
fi

# Ensure that the tag is named properly as a semver tag
if [[ ! "$TRAVIS_TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-rc\.[0-9]+)?$ ]]; then
  echo "Tag $TRAVIS_TAG is NOT a valid semver tag (vX.Y.Z), please delete this tag."
  exit 1
fi

# Ensure that the script is being run from the root project directory
PROPERTIES_FILE='gradle.properties'
if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Could not find $PROPERTIES_FILE, are you sure this is being run from the root project directory?"
  echo "PWD: ${PWD}"
  exit 1
fi

# Determine the version being published
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

# Ensure the tag corresponds to the current version
EXPECTED_TAG="v$VERSION"
if [ "$TRAVIS_TAG" != "$EXPECTED_TAG" ]; then
  echo "Attempting to publish Rest.li version $VERSION from tag $TRAVIS_TAG is illegal."
  echo "Please delete this tag and publish instead from tag $EXPECTED_TAG"
  exit 1
fi

# Ensure the commit environment variable exists
if [ -z "$TRAVIS_COMMIT" ]; then
  echo 'Cannot find environment variable named TRAVIS_COMMIT, did the Travis API change?'
  exit 1
fi

# Ensure that the tag commit is an ancestor of master
git fetch origin master:master
git merge-base --is-ancestor $TRAVIS_COMMIT master
if [ $? -ne 0 ]; then
  echo "Tag $TRAVIS_TAG is NOT an ancestor of master!"
  # Abort the deployment if it's not a release candidate tag
  if $RELEASE_CANDIDATE; then
    echo "Since this is a release candidate tag, the deployment will continue."
  else
    echo 'Please delete this tag and instead create a tag off a master commit.'
    exit 1
  fi
fi

# Output something every 9 minutes, otherwise Travis will abort after 10 minutes of no output
while sleep 9m; do echo "[Ping] Keeping Travis job alive ($((SECONDS / 60)) minutes)"; done &
WAITER_PID=$!

# Build and publish to Bintray
echo "All checks passed, attempting to publish Rest.li $VERSION to Bintray..."
./gradlew bintrayUpload
EXIT_CODE=$?

# Kill the waiter job
kill $WAITER_PID

if [ $EXIT_CODE = 0 ]; then
  echo "Successfully published Rest.li $VERSION to Bintray."
else
  # Publish failed, so roll back the upload to ensure this version is completely wiped from the repo
  echo "Publish failed, wiping $VERSION from Bintray..."
  DELETE_VERSION_URL="https://api.bintray.com/packages/linkedin/maven/pegasus/versions/${VERSION}"
  curl -X DELETE --user ${BINTRAY_USER}:${BINTRAY_KEY} --fail $DELETE_VERSION_URL

  if [ $? = 0 ]; then
    echo "Successfully rolled $VERSION back."
    echo 'Please retry the upload by restarting this Travis job.'
  else
    echo "Failed to roll back $VERSION, please manually delete this version from Bintray."
    echo "See: https://bintray.com/linkedin/maven/pegasus/$VERSION"
    echo 'Once this version is deleted, please retry the upload by restarting this Travis job.'
  fi

  exit 1
fi
