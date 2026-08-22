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

## Bundled dataset

The app ships a snapshot of the public production dataset from
`https://irodriguez.io/News-Agregator/data/articles.json`. Its exact provenance and refresh instructions
are recorded here when the snapshot is committed.
