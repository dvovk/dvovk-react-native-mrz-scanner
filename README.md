# react-native-mrz-scanner

## Info
This is test module. Please leave request on vovk.dimon@gmail.com with subject 'improve react-native-mrz-scanner' and I'll modify library if many people need it.

## Getting started

`$ npm install react-native-mrz-scanner --save`

### Mostly automatic installation

`$ react-native link react-native-mrz-scanner`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-mrz-scanner` and add `MrzScanner.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libMrzScanner.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactlibrary.MrzScannerPackage;` to the imports at the top of the file
  - Add `new MrzScannerPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-mrz-scanner'
  	project(':react-native-mrz-scanner').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-mrz-scanner/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-mrz-scanner')
  	```


## Usage
```javascript
import MrzScanner from 'react-native-mrz-scanner';

// TODO: What to do with the module?
MrzScanner;
```
