
# Android In-App Update Library [![Build Status](https://travis-ci.com/dnKaratzas/android-inapp-update.svg?branch=master)](https://travis-ci.com/dnKaratzas/android-inapp-update) [ ![Download](https://api.bintray.com/packages/dkaratzas/maven/android-inapp-update/images/download.svg) ](https://bintray.com/dkaratzas/maven/android-inapp-update/_latestVersion)
  

This is a simple implementation of the Android In-App Update API.   
For more information on InApp Updates you can check the official [documentation](https://developer.android.com/guide/app-bundle/in-app-updates)

[JavaDocs](https://dnkaratzas.github.io/android-inapp-update/javadoc/)  and a [sample app](https://github.com/dnKaratzas/android-inapp-update/tree/master/app/src/main/java/eu/dkaratzas/android/inapp/update/sample) with examples implemented are available.

# Getting Started

## Requirements
* You project should build against Android 4.0 (API level 14) SDK at least.
* In-app updates works only with devices running Android 5.0 (API level 21) or higher.

## Add to project
* Add to your project's root `build.gradle` file:  
```groovy
buildscript {  
    repositories {
        jcenter()  
    }
}
```
* Add the dependency to your app `build.gradle` file
```groovy
dependencies {  
    implementation 'eu.dkaratzas:android-inapp-update:1.0.1'
}
```
  
## Usage

There are two update modes.

-   Flexible _**(default)**_ - Shows the user an upgrade dialog but performs the downloading of the update within the background. This means that the user can continue using our app whilst the update is being downloaded. When the update is downloaded asks the user confirmation to perform the install.
    
-   Immediate - Will trigger a blocking UI until download and installation is finished. Restart is triggered automatically

## Flexible
<img src="https://developer.android.com/images/app-bundle/flexible_flow.png" alt="" width="825"></p>
* With default user confirmation, the UpdateManager is monitoring the flexible update state, provide a default SnackBar that informs the user that installation is ready and requests user confirmation to restart the app.
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    UpdateManager updateManager = UpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
                .resumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .mode(UpdateMode.FLEXIBLE)
                .snackBarMessage("An update has just been downloaded.")
                .snackBarAction("RESTART")
                .handler(this);

    updateManager.checkForAppUpdate();
}
```  

* With custom user confirmation, need to set the `useCustomNotification(true)` and monitor the update for the `UpdateStatus.DOWNLOADED` status.
Then a notification (or some other UI indication) can be used, to inform the user that installation is ready and requests user confirmation to restart the app. The confirmation must call the `completeUpdate()` method to finish the update.
```java
public class FlexibleWithCustomSnackbar extends AppCompatActivity implements UpdateManager.InAppUpdateHandler {
    private static final int REQ_CODE_VERSION_UPDATE = 530;
    private static final String TAG = "FlexibleCustomSnackbar";
    private UpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateManager = UpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
                .resumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .mode(UpdateMode.FLEXIBLE)
                // default is false. If is set to true you,
                // have to manage the user confirmation when
                // you detect the InstallStatus.DOWNLOADED status,
                .useCustomNotification(true)
                .handler(this);

        updateManager.checkForAppUpdate();
    }

    // InAppUpdateHandler implementation
    
    @Override
    public void onStatusUpdate(UpdateStatus status) {
        if (status == UpdateStatus.DOWNLOADED) {

            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

            Snackbar snackbar = Snackbar.make(rootView,
                    "An update has just been downloaded.",
                    Snackbar.LENGTH_INDEFINITE);

            snackbar.setAction("RESTART", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Triggers the completion of the update of the app for the flexible flow.
                    updateManager.completeUpdate();
                }
            });

            snackbar.show();

        }

        Log.d(TAG, "status: " + status.id());
    }
}
```  

## Immediate

<img src="https://developer.android.com/images/app-bundle/immediate_flow.png" alt="" width="528"></p>

To perform an Immediate update,  needs only to set the mode to `IMMEDIATE` and call the `checkForAppUpdate()` method.
```java
updateManager = UpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
            .resumeUpdates(true) // Resume the update, if the update was stalled. Default is true
            .mode(UpdateMode.IMMEDIATE);

updateManager.checkForAppUpdate();
``` 

---

**Note:** You can listen to the `onActivityResult()` callback to know if you need to request another update in case of a failure.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQ_CODE_VERSION_UPDATE) {
        if (resultCode != RESULT_OK) {
            // If the update is cancelled or fails,
            // you can request to start the update again.
            updateManager.checkForAppUpdate();
            
            Log.d(TAG, "Update flow failed! Result code: " + resultCode);
        }
    }
}
```

## Troubleshoot
-   In-app updates works only with devices running Android 5.0 (API level 21) or higher.
-   Testing this won’t work on a debug build. You would need a release build signed with the same key you use to sign your app before uploading to the Play Store. It would be a good time to use the internal testing track.
-   In-app updates are available only to user accounts that own the app. So, make sure the account you’re using has downloaded your app from Google Play at least once before using the account to test in-app updates.
-   Because Google Play can only update an app to a higher version code, make sure the app you are testing as a lower version code than the update version code.
-   Make sure the account is eligible and the Google Play cache is up to date. To do so, while logged into the Google Play Store account on the test device, proceed as follows:
    1.  Make sure you completely [close the Google Play Store App](https://support.google.com/android/answer/9079646#close_apps).
    2.  Open the Google Play Store app and go to the **My Apps & Games** tab.
    3.  If the app you are testing doesn’t appear with an available update, check that you’ve properly [set up your testing tracks](https://support.google.com/googleplay/android-developer/answer/3131213?hl=en).
## License    

    Copyright 2019 Dionysios Karatzas

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


## Contributing  
  
1. Fork it  
2. Create your feature branch (`git checkout -b my-new-feature`)  
3. Commit your changes (`git commit -am 'Add some feature'`)  
4. Push to the branch (`git push origin my-new-feature`)  
5. **Create New Pull Request**
