#!/usr/bin/env bash

# Ensure that this is being run by Travis
if [ "$TRAVIS" != "true" ] || [ "$USER" != "travis" ]; then
  echo "This script should only be run by Travis CI."
  exit 2
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
