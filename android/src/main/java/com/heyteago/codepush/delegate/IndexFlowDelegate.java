package com.heyteago.codepush.delegate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.heyteago.codepush.data.HtCodePushDb;
import com.heyteago.codepush.data.dao.IndexUpdateDao;
import com.heyteago.codepush.data.dao.IndexVersionDao;
import com.heyteago.codepush.data.entity.IndexUpdateEntity;
import com.heyteago.codepush.data.entity.IndexVersionEntity;
import com.heyteago.codepush.util.DownloadUtil;
import com.heyteago.codepush.util.FileHelper;
import com.heyteago.codepush.util.Md5Util;
import com.heyteago.codepush.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IndexFlowDelegate extends FlowDelegate {
    private static final String BASE_STORAGE = "/data/data/";
    private static final String BUNDLE_NAME = "index.android.bundle";
    private Context mContext;
    private IndexUpdateDao mIndexUpdateDao;
    private IndexVersionDao mIndexVersionDao;

    public IndexFlowDelegate(Context context) {
        this.mContext = context;
        mIndexUpdateDao = HtCodePushDb.getDb(context).indexUpdateDao();
        mIndexVersionDao = HtCodePushDb.getDb(context).indexVersionDao();
    }

    /**
     * TODO: 加载bundle成功后，调用该函数，用于判断热更新的成功、失败，以判断是否回滚操作
     */
    @Override
    public void loadBundleSuccess() {

    }

    @Override
    public String getJsBundleFile() {
        // 查询更新表bundle版本，不包括逻辑删除
        IndexUpdateEntity[] updateEntities = mIndexUpdateDao.findByIsDelete(false);
        if (updateEntities.length > 0 && FileHelper.isExists(updateEntities[0].getBundleFile()) && Md5Util.checkMd5(new File(updateEntities[0].getBundleFile()), updateEntities[0].getMd5())) {
            return updateEntities[0].getBundleFile();
        }
        // 查询版本表bundle版本
        IndexVersionEntity[] versionEntities = mIndexVersionDao.findByEnable(true);
        if (versionEntities.length > 0 && FileHelper.isExists(versionEntities[0].getBundleFile()) && Md5Util.checkMd5(new File(versionEntities[0].getBundleFile()), versionEntities[0].getMd5())) {
            return versionEntities[0].getBundleFile();
        }
        return null;
    }

    @Override
    public boolean checkForHotUpdate(int versionCode) throws Throwable {
        try {
            IndexUpdateEntity[] updateEntities = mIndexUpdateDao.findByIsDelete(false);
            if (updateEntities.length > 0) {
                if (!FileHelper.isExists(updateEntities[0].getBundleFile())) {
                    return true;
                }
                return versionCode > updateEntities[0].getVersionCode();
            }
            IndexVersionEntity[] versionEntities = mIndexVersionDao.findByEnable(true);
            if (versionEntities.length > 0) {
                if (!FileHelper.isExists(versionEntities[0].getBundleFile())) {
                    return true;
                }
                return versionCode > versionEntities[0].getVersionCode();
            }
            return true;
        } catch (Exception e) {
            throw new Throwable(e.getMessage());
        }
    }

    @Override
    public boolean checkForAppUpdate(int versionCode) throws Throwable {
        long localVersionCode = Utils.getAppVersionCode(mContext);
        if (localVersionCode == 0) {
            throw new Throwable("Can not get app version code from device");
        }
        return versionCode > localVersionCode;
    }

    @Override
    public void syncHot(final String md5, final int versionCode, final String url, final OnDownloadListener onDownloadListener) {
        try {
            boolean shouldUpdate = checkForHotUpdate(versionCode);
            if (shouldUpdate) {
                DownloadUtil downloadUtil = new DownloadUtil();
                String destFileDir = BASE_STORAGE + mContext.getPackageName() + "bundle/index/" + versionCode;
                downloadUtil.download(url, destFileDir, BUNDLE_NAME, new DownloadUtil.OnDownloadListener() {
                    @Override
                    public void onProgress(int progress) {
                        if (onDownloadListener != null) {
                            onDownloadListener.onProgress(progress);
                        }
                    }

                    @Override
                    public void onSuccess(File file) {
                        // 下载成功
                        // 1. 检查md5
                        // 2. 更新【更新表】
                        boolean md5Equal = Md5Util.checkMd5(file, md5);
                        if (!md5Equal) {
                            if (onDownloadListener != null) {
                                onDownloadListener.onFail(new Exception("文件md5不一致"));
                            }
                            return;
                        }
                        saveUpdateTable(file, versionCode, md5);
                        if (onDownloadListener != null) {
                            onDownloadListener.onSuccess(file);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        if (onDownloadListener != null) {
                            onDownloadListener.onFail(e);
                        }
                    }
                });
            } else {
                if (onDownloadListener != null) {
                    onDownloadListener.onFail(new Exception("不需要更新"));
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            if (onDownloadListener != null) {
                onDownloadListener.onFail(new Exception(throwable.getMessage()));
            }
        }
    }

    @Override
    public void syncAndroidApp(final String md5, final int versionCode, final String url, final OnDownloadListener onDownloadListener) {
        try {
            boolean shouldUpdate = checkForAppUpdate(versionCode);
            if (shouldUpdate) {
                DownloadUtil downloadUtil = new DownloadUtil();
                downloadUtil.download(url, downloadUtil.getCacheDirectory(mContext), null, new DownloadUtil.OnDownloadListener() {
                    @Override
                    public void onProgress(int progress) {
                        if (onDownloadListener != null) {
                            onDownloadListener.onProgress(progress);
                        }
                    }

                    @Override
                    public void onSuccess(File file) {
//                        boolean md5Equal = Md5Util.checkMd5(file, md5);
//                        if (!md5Equal) {
//                            if (onDownloadListener != null) {
//                                onDownloadListener.onFail(new Exception("文件md5不一致"));
//                            }
//                            return;
//                        }
                        if (onDownloadListener != null) {
                            onDownloadListener.onSuccess(file);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        if (onDownloadListener != null) {
                            onDownloadListener.onFail(e);
                        }
                    }
                });
            } else {
                if (onDownloadListener != null) {
                    onDownloadListener.onFail(new Exception("不需要更新"));
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            if (onDownloadListener != null) {
                onDownloadListener.onFail(new Exception(throwable.getMessage()));
            }
        }
    }

    /**
     * 更新【更新表】
     */
    private void saveUpdateTable(File file, int versionCode, String md5) {
        IndexUpdateEntity updateEntity = new IndexUpdateEntity();
        updateEntity.setBundleFile(file.getPath());
        updateEntity.setVersionCode(versionCode);
        updateEntity.setMd5(md5);
        updateEntity.setCreateAt(SimpleDateFormat.getTimeInstance().format(new Date()));
        mIndexUpdateDao.insertEntities(updateEntity);
    }
}
