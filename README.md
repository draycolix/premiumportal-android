# Premium Portal Android App

WebView wrapper for Premium Portal with Netflix integration.

## Features
- Load premiumportal.id in native Android WebView
- Netflix integration via cookie injection from API
- YouTube support
- Bottom navigation (Home, Netflix, YouTube, Refresh)
- Pull-to-refresh
- User-Agent spoofed for Cloudflare bypass

## How it works
1. Login to premiumportal.id in the app
2. Tap "Netflix" button in bottom nav
3. App fetches Netflix session cookies from `consumer-api.premiumportal.id`
4. Cookies are injected into the WebView for netflix.com
5. Netflix opens already logged in with premium account

## Build
```bash
# Open in Android Studio and build, or:
./gradlew assembleRelease
```

Requirements:
- Android Studio Hedgehog+ (2023.1+)
- Android SDK 34
- JDK 17

APK output: `app/build/outputs/apk/release/app-release.apk`
