# AI Quartet — Android Version 1
Android Kotlin/Jetpack Compose app for manually comparing answers from ChatGPT, Claude, Gemini and Perplexity, without APIs.

## Use
1. Enter a question in Ask.
2. Tap Open beside an AI. The app copies your question and opens the AI's official website in a Custom Tab or browser.
3. Log in through your browser, paste the question, and copy the answer.
4. Return to Compare, paste the answer into the matching card, repeat for all providers.
5. Save, reopen, delete or share comparisons within the app.

The app does not submit questions automatically, extract answers, store AI provider credentials, or embed four websites. It shows four manually pasted responses side by side.

## APK build on GitHub
Go to Actions > Build AI Quartet APK > Run workflow. A push to main also triggers a build. After the build succeeds, open that run and download the AIQuartet-debug-APK artifact, unzip and install app-debug.apk.

The build has not been verified until GitHub Actions finishes successfully. Android Studio alternatively requires JDK 17, Android SDK 35 and Gradle 8.9.

Privacy: saved questions and responses use app-private local storage; provider logins remain in the user's browser. Text you copy is briefly exposed to the Android clipboard.
