# @heytea/react-native-heytea-codepush

[![GitHub license](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)
[![npm](https://img.shields.io/badge/npm-1.0.1-green)](https://www.npmjs.com/package/@heytea/react-native-heytea-codepush)



## Getting started


`$ npm install @heytea/react-native-heytea-codepush --save`

### Mostly automatic installation

`$ react-native link @heytea/react-native-heytea-codepush`

## Usage

### ReactNative

```javascript
import ReactNativeHeyteaCodePush from 'react-native-heytea-codepush';

/**
 * 运行bundle过程中错误，调用该函数，用于判断热更新的成功、失败，以判断是否回滚操作
 * 目前只支持安卓
 */
ReactNativeHeyteaCodePush.loadFail(): void;

/**
 * 检查是否需要热更新
 * @param versionCode 热更新文件的versionCode
 */
ReactNativeHeyteaCodePush.checkForHotUpdate(versionCode: number): Promise<boolean>;

/**
 * 检查是否需要App更新
 * @param versionCode App的versionCode, iOS对应versionNumber
 */
ReactNativeHeyteaCodePush.checkForAppUpdate(versionCode: number): Promise<boolean>;

/**
 * 同步热更新，与微软CodePush基本一致
 */
ReactNativeHeyteaCodePush.syncHot(
    restartAfterUpdate: boolean,
    md5: string,
    versionCode: number,
    url: string,
    callback: (success?: boolean, error?: string) => void
): void;

/**
 * 同步安卓App更新，下载apk并提示安装
 */
ReactNativeHeyteaCodePush.syncAndroidApp(
    md5: string,
    versionCode: number,
    url: string,
    callback: (success?: boolean, error?: string) => void
): void;

/**
 * 同步iOS App更新，跳转App store
 * @param {string} url App Store应用链接
 */
ReactNativeHeyteaCodePush.synciOSApp(url: string): void;

/**
 * 监听下载进度
 */
componentDidMount() {
  this.progressEmitter = eventEmitter.addListener('syncProgress', (progress: number) => {

  })
}

componentWillMount() {
  this.progressEmitter && this.progressEmitter.remove()
}

```

### Android

```java
public class MainApplication extends Application implements ReactApplication {
    private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
        ...
        // 2. Override the getJSBundleFile method in order to let
        // the CodePush runtime determine where to get the JS
        // bundle location from on each app start
        @Override
        protected String getJSBundleFile() {
            return HeyteaCodePush.getJSBundleFile(MainApplication.this);
        }
    };
}
```

### iOS

```c
- (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
{
  #if DEBUG
    return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
  #else
    return [HeyteaCodePush bundleURL];
  #endif
}
```
