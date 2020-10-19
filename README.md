# Gyroscope-Based Time Synchronization of Distributed Cameras

We use a modified version of [libsoftwaresync](https://github.com/google-research/libsoftwaresync) to verify sync accuracy by taking pictures on two devices with minimal delay. You can learn more about phase alignment implementation, capture details in [libsoftwaresync](https://github.com/google-research/libsoftwaresync) or in this paper:
[Wireless Software Synchronization of Multiple Distributed Cameras](https://arxiv.org/abs/1812.09366).
_Sameer Ansari, Neal Wadhwa, Rahul Garg, Jiawen Chen_, ICCP 2019.

### Our main contribution to the app

- We integrated gyroscope-based time synchronization algorithm instead of SNTP to demonstrate sync accuracy. The algoritm requires client and leader smartphone to be firmly fixed together and shaken for a few seconds. After that the recorded gyroscope data is processed and calculated offset is returned to the client smartphone.

### Installation instructions:

#### Gyro-based sync server setup

1.  Make sure that your pc is connected to the same network as all the smartphones.
2.  Before using the app, run ```python3 sync-server/server.py```. This command launches simple TCP-based server which accepts two gyroscope files, calculates and returns offset in Ns to the leader smartphone.
3.  Specify pc host address in ```Constants``` class of AndroidStudio project (address can be found with ```ifconfig```)

#### App setup

1.  Download [Android Studio](https://developer.android.com/studio). When you
    install it, make sure to also install the Android SDK API 27.
2.  Click "Open an existing Android Studio project". Select the "CaptureSync"
    directory.
3.  There will be a pop-up with the title "Gradle Sync" complaining about a
    missing file called gradle-wrapper.properties. Click ok to recreate the
    Gradle wrapper.
4.  Modify the following values in ```Constants.java```: ```PC_SERVER_IP``` - IP of gyro sync server, can be obtained from the script output or with ifconfig, ```LEADER_IP``` - IP of leader smartphone.
5.  Plug in your smartphone. You will need to enable USB debugging. See
    https://developer.android.com/studio/debug/dev-options for further
    instructions.
6.  Go to the "Run" menu at the top and click "Run 'app'" to compile and install
    the app.

Note: By default, the app will likely start in client mode, with no UI options.


### App basic usage

1. User sets up all devices on the same WiFi network.
2. User starts app on all devices, uses exposure sliders, presses the ```Start Syncing``` button on leader device. After that, user is forced to fix devices and shake them during recording (recording start and end are marked by beep sounds on leader smartphone).
3. Then user presses ```Phase Align``` button on the leader device.
3. User presses capture button on the leader device to collect captures.
4. If JPEG is enabled (default) the user can verify captures by going to the Pictures photo directory on their phone through Google Photos or similar.
5. After a capture session, the user pulls the data from each phone to the local machine using ```adb pull```.

