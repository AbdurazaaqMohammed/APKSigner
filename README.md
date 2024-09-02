# APK Signer
Simple Android app to sign APKs, supports signing split APKs and multiple files.

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
