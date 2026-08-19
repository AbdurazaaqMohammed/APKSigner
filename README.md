# APK Signer
Simple Android app to sign APKs, supports signing split APKs and multiple files.

# Note

This app is really outdated all features and more have been implemented with better interface, speed and functionality (Support JKS & creating new key) in my new app <a href="https://github.com/AbdurazaaqMohammed/MP-Manager">MP Manager</a>, Try it instead

<p align="center">
  <img src="https://github.com/user-attachments/assets/fbd4da0d-3ade-46b2-a79e-b55c697b1aa9" width="250" alt="Screenshot of Sign APK Dialog in MP Manager">
  <br>
  Improved APK Signer in <a href="https://github.com/AbdurazaaqMohammed/MP-Manager">MP Manager</a>
</p>

# Usage

There are 3 ways to open the APK to sign:
* Share the file and select Sign APK in the share menu
* Press (open) the file and select Sign APK in available options
* Open the app from launcher and press the button then select the APK file(s).

Note: Some apps verify the signature of the APK or take other measures to check if the app was modified, which may cause it to crash on startup.

# Used projects
⭐ [Android port of apksig library](https://github.com/MuntashirAkon/apksig-android) by [MuntashirAkon](https://github.com/MuntashirAkon) to sign APKs

* Apache Commons Compress
* PseudoApkSigner by Aefyr for backup signing on older Android versions
* AmbilWarna Color Picker
* android-filepicker by Angad Singh for file picker on older Android versions

# Todo
* Support v4 signature scheme
