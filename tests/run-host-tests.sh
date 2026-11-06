#!/usr/bin/env bash
set -euo pipefail
project="$(cd -- "$(dirname -- "$0")/.." && pwd)"
: "${RIN23_JSON_JAR:?Set RIN23_JSON_JAR to org.json json-20240303.jar}"
mkdir -p "$project/build/host-tests"
java com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$RIN23_JSON_JAR" -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/RemotePolicy.java" "$project/app/src/main/java/com/rin23/app/Protocol.java" "$project/app/src/main/java/com/rin23/app/CatalogCache.java" "$project/app/src/main/java/com/rin23/app/CatalogClient.java" "$project/app/src/main/java/com/rin23/app/SearchQuery.java" "$project/app/src/main/java/com/rin23/app/FeedState.java" "$project/app/src/main/java/com/rin23/app/TransferMeter.java" "$project/tests/ProtocolTest.java" "$project/tests/CatalogClientTest.java" "$project/tests/FeedTransferTest.java"
for test in ProtocolTest CatalogClientTest FeedTransferTest; do java -cp "$project/build/host-tests:$RIN23_JSON_JAR" "com.rin23.app.$test"; done

java com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$RIN23_JSON_JAR:$project/deps/zxing-core-3.5.3.jar" -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/AppVersion.java" "$project/app/src/main/java/com/rin23/app/DonationClient.java" "$project/app/src/main/java/com/rin23/app/VoucherQrReader.java" "$project/tests/DonationClientTest.java" "$project/tests/VoucherQrTest.java"
for test in DonationClientTest VoucherQrTest; do java -cp "$project/build/host-tests:$RIN23_JSON_JAR:$project/deps/zxing-core-3.5.3.jar" "com.rin23.app.$test"; done

java com.sun.tools.javac.Main --release 8 -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/DownloadSession.java" "$project/app/src/main/java/com/rin23/app/GalleryRules.java" "$project/tests/DetailsStateTest.java"
java -cp "$project/build/host-tests" com.rin23.app.DetailsStateTest

java com.sun.tools.javac.Main --release 8 -classpath "$project/build/host-tests" -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/DownloadHistory.java" "$project/tests/DownloadRegressionTest.java"
java -cp "$project/build/host-tests" com.rin23.app.DownloadRegressionTest

java com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$RIN23_JSON_JAR:$project/build/host-tests" -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/FavoriteStore.java" "$project/app/src/main/java/com/rin23/app/FileSearch.java" "$project/tests/FavoritesSearchTest.java"
java -cp "$project/build/host-tests:$RIN23_JSON_JAR" com.rin23.app.FavoritesSearchTest

java com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$RIN23_JSON_JAR:$project/build/host-tests" -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/PackArchive.java" "$project/app/src/main/java/com/rin23/app/RandomDiscovery.java" "$project/tests/PackArchiveTest.java" "$project/tests/SearchDiscoveryTest.java"
for test in PackArchiveTest SearchDiscoveryTest; do java -cp "$project/build/host-tests:$RIN23_JSON_JAR" "com.rin23.app.$test"; done

java com.sun.tools.javac.Main --release 8 -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/ThemePalette.java" "$project/tests/ThemePaletteTest.java"
java -cp "$project/build/host-tests" com.rin23.app.ThemePaletteTest

java com.sun.tools.javac.Main --release 8 -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/IntegrityPolicy.java" "$project/app/src/main/java/com/rin23/app/SharePolicy.java" "$project/tests/SecurityPolicyTest.java"
java -cp "$project/build/host-tests" com.rin23.app.SecurityPolicyTest

java com.sun.tools.javac.Main --release 8 -d "$project/build/host-tests" "$project/app/src/main/java/com/rin23/app/RemotePolicy.java" "$project/app/src/main/java/com/rin23/app/KeyDataPolicy.java" "$project/tests/RemoteDataTest.java"
java -cp "$project/build/host-tests" com.rin23.app.RemoteDataTest

java com.sun.tools.javac.Main --release 8 -classpath "$RIN23_JSON_JAR:$project/build/host-tests" -d "$project/build/host-tests" "$project/tests/StartupCacheTest.java"
java -cp "$project/build/host-tests:$RIN23_JSON_JAR" com.rin23.app.StartupCacheTest
