# Intentional Reading for Android

This directory is an independent Gradle root for the native Android client. The client is a read-only
consumer of the frozen `ArticleDataset v1` contract and does not change the HTML, CSS, JavaScript, or
Python application. The Pages build allowlists only the web runtime, so `/android` is excluded from the
deployed GitHub Pages artifact.

## Local build

Use Android Studio's bundled JBR because this repository does not require a system Java installation:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
printf 'sdk.dir=%s\n' "$HOME/Library/Android/sdk" > local.properties
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

`local.properties` is machine-specific and ignored by Git.

## Test fixture provenance

The unit suite uses a frozen snapshot of the public production dataset from
`https://irodriguez.io/News-Agregator/data/articles.json`.

The committed fixture is not shipped in the APK. It was fetched on 2026-08-22, reports `generatedAt`
`2026-08-22T12:59:34Z`, and
the response ETag was `"6a899d51-29708"`. Its SHA-256 is
`235e4df614b66108d1a471dddfa0b3ce06d838ac058d8570d440a5d7ac93f27f`.

Refresh it from the repository root without editing or trimming the response:

```sh
curl -fsS -o android/app/src/test/resources/sample_articles.json \
  https://irodriguez.io/News-Agregator/data/articles.json
```

After a refresh, update this provenance and the snapshot profile in
`specs/002-android-client-foundation/evidence.md`, then run both Android gates. The unit suite rejects
invalid UTF-8 or a contract violation in the frozen fixture.
