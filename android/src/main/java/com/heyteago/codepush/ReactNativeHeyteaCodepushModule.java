package com.heyteago.codepush;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.facebook.react.bridge.ActivityEventListener;
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
    private File currentApk;

    public ReactNativeHeyteaCodepushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        mFlowDelegate = HeyteaCodePush.getFlowDelegate(reactContext);
        reactContext.addActivityEventListener(new ActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                if (requestCode == 10086 && currentApk != null) {
                    installApp(currentApk);
                }
            }

            @Override
            public void onNewIntent(Intent intent) {

            }
        });
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
                        final Callback callback) {
        mFlowDelegate.syncHot(md5, versionCode, url, new FlowDelegate.OnDownloadListener() {
            @Override
            public void onProgress(int progress) {
                if (callback != null) {
                    callback.invoke(progress, null, null);
                }
            }

            @Override
            public void onSuccess(File file) {
                if (callback != null) {
                    callback.invoke(100, true, null);
                }
                if (restartAfterUpdate) {
                    restartApp();
                }
            }

            @Override
            public void onFail(Exception e) {
                if (callback != null) {
                    callback.invoke(null, false, e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void syncAndroidApp(String md5,
                               int versionCode,
                               String url,
                               final Callback callback) {
        mFlowDelegate.syncAndroidApp(md5, versionCode, url, new FlowDelegate.OnDownloadListener() {
            @Override
            public void onProgress(int progress) {
                if (callback != null) {
                    callback.invoke(progress, null, null);
                }
            }

            @Override
            public void onSuccess(File file) {
                if (callback != null) {
                    callback.invoke(100, true, null);
                }
                installApp(file);
            }

            @Override
            public void onFail(Exception e) {
                if (callback != null) {
                    callback.invoke(null, false, e.getMessage());
                }
            }
        });
    }

    private void restartApp() {
        Intent intent = getCurrentActivity().getIntent();
        getCurrentActivity().finish();
        getCurrentActivity().startActivity(intent);
    }

    private void installApp(File apk) {
        try {
            currentApk = apk;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 是否有安装位置来源的权限
                boolean haveInstallPermission = getCurrentActivity().getPackageManager().canRequestPackageInstalls();
                if (!haveInstallPermission) {
                    Uri packageUri = Uri.parse("package:" + reactContext.getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
                    getCurrentActivity().startActivityForResult(intent, 10086);
                    return;
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(getCurrentActivity(), reactContext.getPackageName() + ".fileprovider", apk);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromFile(apk);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            getCurrentActivity().startActivity(intent);
        } catch (Exception e) {
            Log.e("HeyteaCodePush", e.getMessage());
        }
    }
}
