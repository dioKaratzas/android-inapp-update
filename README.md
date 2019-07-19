
# Android In-App Update Library [![Build Status](https://travis-ci.com/dnKaratzas/android-inapp-update.svg?branch=master)](https://travis-ci.com/dnKaratzas/android-inapp-update) [ ![Download](https://api.bintray.com/packages/dkaratzas/maven/android-inapp-update/images/download.svg?version=1.0.0) ](https://bintray.com/dkaratzas/maven/android-inapp-update/1.0.0/link)  
  

This is a simple implementation of the Android In-App Update API.   
For more information on InApp Updates you can check the official [documentation](https://developer.android.com/guide/app-bundle/in-app-updates)

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
    implementation 'eu.dkaratzas:android-inapp-update:1.0.0'
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
                .setResumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .setMode(UpdateMode.FLEXIBLE)
                .setUseDefaultSnackbar(true) //default is true
                .setSnackBarMessage("An update has just been downloaded.")
                .setSnackBarAction("RESTART")
                .setHandler(this);

        updateManager.checkForAppUpdate();
    }
```  

* With custom user confirmation, need to set the `setUseDefaultSnackbar(false)` and monitor the update for the `UpdateStatus.DOWNLOADED` status.
Then a notification (or some other UI indication) can be used, to inform the user that installation is ready and requests user confirmation to restart the app. The confirmation must call the `updateManager.completeUpdate();` method to finish the update.
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
                .setResumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .setMode(UpdateMode.FLEXIBLE)
                //default is true. If is set to false you,
                // have to manage the user confirmation when
                // you detect the InstallStatus.DOWNLOADED status,
                .setUseDefaultSnackbar(false)
                .setHandler(this);

        updateManager.checkForAppUpdate();
    }

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
```java
    updateManager = UpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
                .setResumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .setMode(UpdateMode.IMMEDIATE);

    updateManager.checkForAppUpdate();
``` 

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
