package com.heyteago.codepush.util;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtil {
    private OkHttpClient mOkHttpClient;
    private Set<Call> callSet = new HashSet<>();

    public DownloadUtil() {
        mOkHttpClient = new OkHttpClient();
    }

    public interface OnDownloadListener {
        void onProgress(int progress);

        void onSuccess(File file);

        void onFail(Exception e);
    }

    public String getCacheDirectory(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            if (context.getExternalCacheDir() == null) {
                return Environment.getExternalStorageDirectory() + File.separator + "Android/data/"
                        + context.getPackageName() + File.separator + "cache";
            }
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }


    public void download(String url, final String destFileDir, @Nullable final String destFileName, final OnDownloadListener onDownloadListener) {
        String fileName = destFileName;
        if (fileName == null) {
            String[] fileDirArr = url.split("/");
            fileName = fileDirArr[fileDirArr.length - 1];
        }
        Request request = new Request.Builder()
                .url(url)
                .build();

        //储存下载文件的目录
        final File dir = new File(destFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final String finalFileName = fileName;

        Call call = mOkHttpClient.newCall(request);
        callSet.add(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                onDownloadListener.onFail(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;

                if (response.body() == null) {
                    onDownloadListener.onFail(new Exception("response.body() is null"));
                    return;
                }

                File file = new File(dir, finalFileName);

                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    int lastProgress = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        final int progress = (int) (sum * 1.0f / total * 100);
                        if (lastProgress != progress) {
                            lastProgress = progress;
                            //下载中更新进度条
                            onDownloadListener.onProgress(progress);
                        }
                    }
                    fos.flush();
                    final File finalFile = file;
                    onDownloadListener.onSuccess(finalFile);
                } catch (final Exception e) {
                    onDownloadListener.onFail(e);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {

                    }

                }
            }
        });
    }

    public void cancel() {
        for (Call call : callSet) {
            call.cancel();
        }
    }
}
