package com.heyteago.codepush.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadUtil {
    private static final String TAG = DownloadUtil.class.getSimpleName();

    private final OkHttpClient mOkHttpClient;
    private final Set<Call> callSet = new HashSet<>();
    private final ExecutorService mPoolExecutor = new ThreadPoolExecutor(3, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private volatile boolean flag = false;

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

    public int download(final String url, final String destFileDir, final String destFileName, final OnDownloadListener onDownloadListener) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int id = hashCode();
                Log.d(TAG, "Runnable run hashCode: " + id);
                try {
                    flag = true;
                    //下载的文件名称
                    String fileName = destFileName;
                    if (fileName == null) {
                        String[] fileDirArr = url.split("/");
                        fileName = fileDirArr[fileDirArr.length - 1];
                    }
                    //判断本地是否有断点下载缓存文件
                    String cacheFileName = fileName + ".cache";
                    File cacheFile = new File(destFileDir, cacheFileName);
                    RandomAccessFile tmpAccessFile;
                    Call call;
                    long totalLength = 0;
                    if (cacheFile.exists()) {
                        tmpAccessFile = new RandomAccessFile(cacheFile, "rwd");
                        tmpAccessFile.seek(cacheFile.length());
                        long offset = cacheFile.length();
                        long total = getContentLength(url);
                        Log.d(TAG, "offset: " + offset + ", total: " + total);
                        call = newRangCall(url, offset, total);
                        totalLength = cacheFile.length();
                    } else {
                        tmpAccessFile = new RandomAccessFile(cacheFile, "rwd");
                        call = newCall(url);
                    }
                    // 请求
                    Response response = call.execute();
                    int code = response.code();
                    ResponseBody body = response.body();
                    if (body == null) {
                        onDownloadListener.onFail(new Exception("response.body() is null"));
                        return;
                    }
                    if (code == 206 || code == 200) {
                        InputStream is = body.byteStream();
                        byte[] buf = new byte[2048];
                        int len;
                        long sum = totalLength;
                        // 如果返回200，则不支持断点下载
                        if (code == 200) {
                            sum = 0;
                            totalLength = 0;
                            tmpAccessFile.seek(0);
                        }
                        Log.d(TAG, "response body length: " + body.contentLength());
                        totalLength = totalLength + body.contentLength();
                        while (flag && (len = is.read(buf)) != -1) {
                            tmpAccessFile.write(buf, 0, len);
                            sum += len;
                            int progress = (int) (sum * 1.0f / totalLength * 100);
                            onDownloadListener.onProgress(progress);
                        }
                        is.close();
                        tmpAccessFile.close();

                        if (sum >= totalLength) {
                            // 如果之前存在相同的文件，先删除
                            File beforeFile = new File(destFileDir, fileName);
                            if (beforeFile.exists()) {
                                beforeFile.delete();
                            }
                            // 重命名 .cache 文件
                            File currentFile = new File(destFileDir, cacheFileName);
                            File newFile = new File(destFileDir, cacheFileName.substring(0, cacheFileName.length() - 6));
                            currentFile.renameTo(newFile);

                            onDownloadListener.onSuccess(newFile);
                        }
                    } else {
                        onDownloadListener.onFail(new Exception("response.code() is not 200 or 206"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    onDownloadListener.onFail(e);
                }
            }
        };
        int jobId = runnable.hashCode();
        Log.d(TAG, "runnable hashCode: " + jobId);
        mPoolExecutor.execute(runnable);
        return jobId;
    }

    public void stop(int jobId) {
        flag = false;
    }

    public void resume(int jobId) {

    }

    public void cancel(int jobId) {

    }

    public void cancelAll() {

    }

    private Call newRangCall(String url, long offset, long total) {
        Request request = new Request.Builder()
                .url(url)
                .header("Range", "bytes=" + offset + "-" + total)
                .build();
        return mOkHttpClient.newCall(request);
    }

    private Call newCall(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return mOkHttpClient.newCall(request);
    }

    private long getContentLength(String downloadUrl) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        try {
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (response.isSuccessful() && body != null) {
                long contentLength = body.contentLength();
                body.close();
                return contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}