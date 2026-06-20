#!/bin/bash
# Run this to create the GitHub Actions workflow
mkdir -p .github/workflows
cat > .github/workflows/build.yml << 'WORKFLOW'
name: Build APK

on:
  push:
    branches: [ master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.5'
      - name: Install Android SDK
        run: |
          sudo apt-get update -qq
          sudo apt-get install -y android-sdk
          echo "ANDROID_HOME=/usr/lib/android-sdk" >> $GITHUB_ENV
          mkdir -p /usr/lib/android-sdk/licenses
          echo "8933bad161af4178b1185d1a37fbf41ea5269c55" > /usr/lib/android-sdk/licenses/android-sdk-license
      - name: Build APK
        run: gradle assembleDebug --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: premium-portal-apk
          path: app/build/outputs/apk/debug/*.apk
WORKFLOW
echo "Workflow created! Now commit and push."
