package com.heyteago.codepush.delegate;

import android.content.Context;

import java.io.File;

public abstract class FlowDelegate {

    public abstract void loadBundleSuccess();

    public abstract String getJsBundleFile();

    public abstract boolean checkForHotUpdate(int versionCode) throws Throwable;

    public abstract boolean checkForAppUpdate(int versionCode) throws Throwable;

    public abstract void syncHot(String md5, int versionCode, String url, OnDownloadListener onDownloadListener);

    public abstract void syncAndroidApp(String md5, int versionCode, String url, OnDownloadListener onDownloadListener);

    public interface OnDownloadListener {
        void onProgress(int progress);

        void onSuccess(File file);

        void onFail(Exception e);
    }
}