#!/usr/bin/env bash
set -eou pipefail

./gradlew build shadowJar -x check

java -jar sample/build/libs/sample-all.jar -Xms1024m -Xmx1024m