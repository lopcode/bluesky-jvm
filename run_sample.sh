#!/usr/bin/env bash
set -eou pipefail

./gradlew build shadowJar

java -jar sample/build/libs/sample-all.jar -Xms4096m -Xmx4096m