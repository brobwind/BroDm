# BroDm
An Android app used to manipulate Brillo system webservice for debugging


 ![](http://www.brobwind.com/wp-content/uploads/2016/02/2016_02_20_brobdm_main.png)

## Rebuild jni/libs/librokit.a
	$ mkdir -p /local/brillo-m9-dev && cd /local/brillo-m9-dev
	$ repo init -u https://android.googlesource.com/brillo/manifest -b brillo-m9-dev
	$ repo sync
	$ . build/envsetup.sh
	$ lunch brilloemulator_arm-eng
	$ mkdir -pv packages/apps && cd packages/apps
	$ git clone https://github.com/brobwind/BroDm.git
	$ cd BroDm/jni/libs && mma -j 8

 After all the jni/libs/libbrokit.a will be updated

## Build BroDm APK
	$ mkdir -pv /local/android-5.1.1_r15 && cd /local/android-5.1.1_r15
	$ repo init -u https://android.googlesource.com/platform/manifest -b android-5.1.1_r15
	$ repo sync
	$ . build/envsetup.sh
	$ lunch aosp_arm-eng
	$ cd packages/apps
	$ git clone https://github.com/brobwind/BroDm.git
	$ cd BroDm && mma -j 8

 After all the APK will be installed in:
	/loca/android-5.1.1_r15/out/target/product/generic/data/app/BroDm/BroDm.apk

## Others
 Please visit:
	http://www.brobwind.com/archives/628
