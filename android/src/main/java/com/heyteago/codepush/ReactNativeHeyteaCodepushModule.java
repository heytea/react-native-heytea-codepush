package com.heyteago.codepush;

import android.content.Intent;
import android.net.Uri;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.heyteago.codepush.delegate.FlowDelegate;

import java.io.File;

public class ReactNativeHeyteaCodepushModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private FlowDelegate mFlowDelegate;

    public ReactNativeHeyteaCodepushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        mFlowDelegate = HeyteaCodePush.getFlowDelegate(reactContext);
    }

    @Override
    public String getName() {
        return "ReactNativeHeyteaCodepush";
    }

    @ReactMethod
    public void loadSuccess() {
        mFlowDelegate.loadBundleSuccess();
    }

    @ReactMethod
    public void checkForHotUpdate(int versionCode, Promise promise) {
        try {
            boolean shouldUpdate = mFlowDelegate.checkForHotUpdate(versionCode);
            promise.resolve(shouldUpdate);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            promise.reject(throwable);
        }
    }

    @ReactMethod
    public void checkForAppUpdate(int versionCode, Promise promise) {
        try {
            boolean shouldUpdate = mFlowDelegate.checkForAppUpdate(versionCode);
            promise.resolve(shouldUpdate);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            promise.reject(throwable);
        }
    }

    @ReactMethod
    public void syncHot(final boolean restartAfterUpdate,
                        String md5,
                        int versionCode,
                        String url,
                        final Callback progressCallback,
                        final Callback successCallback,
                        final Callback errorCallback) {
        mFlowDelegate.syncHot(md5, versionCode, url, new FlowDelegate.OnDownloadListener() {
            @Override
            public void onProgress(int progress) {
                if (progressCallback != null) {
                    progressCallback.invoke(progress);
                }
            }

            @Override
            public void onSuccess(File file) {
                if (successCallback != null) {
                    successCallback.invoke();
                }
                if (restartAfterUpdate) {
                    restartApp();
                }
            }

            @Override
            public void onFail(Exception e) {
                if (errorCallback != null) {
                    errorCallback.invoke(new Throwable(e.getMessage()));
                }
            }
        });
    }

    @ReactMethod
    public void syncAndroidApp(String md5,
                               int versionCode,
                               String url,
                               final Callback progressCallback,
                               final Callback successCallback,
                               final Callback errorCallback) {
        mFlowDelegate.syncAndroidApp(md5, versionCode, url, new FlowDelegate.OnDownloadListener() {
            @Override
            public void onProgress(int progress) {
                if (progressCallback != null) {
                    progressCallback.invoke(progress);
                }
            }

            @Override
            public void onSuccess(File file) {
                if (successCallback != null) {
                    successCallback.invoke();
                }
                installApp(file.getPath());
            }

            @Override
            public void onFail(Exception e) {
                if (errorCallback != null) {
                    errorCallback.invoke(new Throwable(e.getMessage()));
                }
            }
        });
    }

    private void restartApp() {
        Intent intent = getCurrentActivity().getIntent();
        getCurrentActivity().finish();
        getCurrentActivity().startActivity(intent);
    }

    private void installApp(String apkPath) {
        Uri uri = Uri.parse(apkPath);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getCurrentActivity().startActivity(intent);
    }
}
