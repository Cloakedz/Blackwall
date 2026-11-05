# Blackwall for Android

A private build of Blackwall for your own phone. It isn't published anywhere: you build the APK and install it yourself. Tabs, sign-ins and settings stay on the phone, and the app opts out of Android backups and phone-to-phone transfer.

## Features

- Tabs, which reopen when you come back
- Ad and tracker blocking (the same built-in list as desktop Blackwall)
- Pop-ups blocked unless you tapped something
- Fullscreen video, downloads to your Downloads folder, and sharing
- Can be set as your default browser

## Build and install

1. Install Android Studio (it includes the JDK and Android SDK).
2. In Android Studio, choose **Open** and select this `mobile` folder. Let Gradle sync finish.
3. On your phone, turn on **Developer options** and **USB debugging**, then plug it in.
4. Press **Run**. Android Studio builds the app and installs it on your phone.

To get an APK file instead, use **Build > Generate App Bundles or APKs > Generate APKs**. Copy the file to your phone and open it. You'll need to allow installs from that app.

Requirements: Android 11 or newer, Android Gradle plugin 9.4.0, Gradle 9.6.0 and JDK 17.
