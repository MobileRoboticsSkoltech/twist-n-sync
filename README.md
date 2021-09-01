# Gyroscope-Based Time Synchronization of Distributed Cameras
[![Android CI on Ubuntu and MacOS](https://github.com/pmokeev/twist-n-sync/actions/workflows/main-ubuntuAndMacOS.yml/badge.svg)](https://github.com/pmokeev/twist-n-sync/actions/workflows/main-ubuntuAndMacOS.yml)

[![Android CI on Windows](https://github.com/pmokeev/twist-n-sync/actions/workflows/main-windows.yml/badge.svg)](https://github.com/pmokeev/twist-n-sync/actions/workflows/main-windows.yml)

We use a modified version of [libsoftwaresync](https://github.com/google-research/libsoftwaresync) to verify sync accuracy by taking pictures on two devices with minimal delay. You can learn more about phase alignment implementation, capture details in [libsoftwaresync](https://github.com/google-research/libsoftwaresync) or in this paper:
[Wireless Software Synchronization of Multiple Distributed Cameras](https://arxiv.org/abs/1812.09366).
_Sameer Ansari, Neal Wadhwa, Rahul Garg, Jiawen Chen_, ICCP 2019.

### Major update (summer 2021)

- **Gyroscope-based time synchronization algorithm** is now available as a standalone C++ library! Author: [Arthur Chains](https://github.com/achains)
- Additionally, the library was **integrated into the application** instead of the old setup requiring PC server. Author of the integration: [Pavel Mokeev](https://github.com/pmokeev)

The library is publicly available on [Github](https://github.com/achains/Twist-n-Sync-CPP-Module). [This presentation](https://docs.google.com/presentation/d/1gX3Kj3gN3wH_CyyOZ-pAtRu5GJzXibgBMo_3uWvNeQc/edit#slide=id.ge6af606f85_0_5) contains more info on the update, as well as some performance tests and details of the implementation.

### Our main contribution to the app

- We integrated **gyroscope-based time synchronization algorithm** instead of SNTP to demonstrate sync accuracy. The algoritm requires client and leader smartphone to be rigidly attached together and shaken for a few seconds. After that the recorded gyroscope data is processed and calculated offset is returned to the client smartphone. All the experiments have been carried out using two Samsung S10e smartphones.

### Complete report

For further reference on the twist-n-sync implementation and complete report, see our [publication here](https://www.mdpi.com/1424-8220/21/1/68):

Faizullin M, Kornilova A, Akhmetyanov A, Ferrer G. Twist-n-Sync: Software Clock Synchronization with Microseconds Accuracy Using MEMS-Gyroscopes. Sensors. 2021; 21(1):68. 


```
@article{faizullin,
  title={Twist-n-Sync: Software Clock Synchronization with Microseconds Accuracy Using MEMS-Gyroscopes},
  author={Faizullin, Marsel and Kornilova, Anastasiia and Akhmetyanov, Azat and Ferrer, Gonzalo},
  journal={Sensors},
  volume={21},
  number={1},
  year={2021}
}
```


### Demonstration of sync accuracy

![Demonstration of capture sync on notebook flipping](https://imgur.com/NjwgfzS.jpg) \
_Demonstration of gyroscope-based sync accuracy on notebook flipping, shot by two Galaxy S10e smartphones_

![Demonstration of capture sync on snowballs, photos](https://imgur.com/odnkuFN.jpg) \
_Experiment with snowballs, photos shot by two Samsung Galaxy S10e smartphones_

![Demonstration of capture sync on snowballs, footage](https://imgur.com/kWs0iA8.gif) \
_Footage of experiment with snowballs_

### Installation

#### App setup

1.  Download [Android Studio](https://developer.android.com/studio). When you
    install it, make sure to also install the Android SDK API 27.
2.  Click "Open an existing Android Studio project". Select the "CaptureSync"
    directory.
3.  There will be a pop-up with the title "Gradle Sync" complaining about a
    missing file called gradle-wrapper.properties. Click ok to recreate the
    Gradle wrapper.
4.  Modify the following values in ```Constants.java```: ```PC_SERVER_IP``` - IP of gyro sync server, can be obtained from the script output or with ifconfig, ```LEADER_IP``` - IP of leader smartphone. You can also try changing gyro sampling period in ```GYRO_PERIOD_US``` (value in microseconds)
5.  Change ```periodNs``` value in ```res/raw/default_phaseconfig.json``` to the image capturing period of your smartphone in nanoseconds. For now, this value should obtained empirically as a mean period between frames.
6.  Plug in your smartphone. You will need to enable USB debugging. See
    https://developer.android.com/studio/debug/dev-options for further
    instructions.
7.  Go to the "Run" menu at the top and click "Run 'app'" to compile and install
    the app.

Note: By default, the app will likely start in client mode, with no UI options.


### Usage

1. **Set up devices** on the same WiFi network.
2. **Start app** on devices, use exposure sliders, press the ```Start Syncing``` button on the leader device. 
3. After that, app shows a dialog asking to **rigidly connect devices and shake them during gyroscope recording** (recording start and end are marked by beep sounds on leader smartphone). Sync result is displayed shortly after the signal as a Toast on the leader device.
4. Then press ```Phase Align``` button on the leader device.
5. Press **capture button** on the leader device to collect captures.
6. If JPEG is enabled (default) the user can verify captures by going to the Pictures photo directory on their phone through Google Photos or similar.
7. After a capture session, the data from each phone can be pulled to the pc with ```adb pull```.

