package com.heyteago.codepush.delegate;

import android.content.Context;
import com.heyteago.codepush.data.HtCodePushDb;
import com.heyteago.codepush.data.dao.IndexUpdateDao;
import com.heyteago.codepush.data.dao.TempDao;
import com.heyteago.codepush.data.entity.IndexUpdateEntity;
import com.heyteago.codepush.data.entity.TempEntity;
import com.heyteago.codepush.util.DownloadUtil;
import com.heyteago.codepush.util.FileHelper;
import com.heyteago.codepush.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IndexFlowDelegate extends FlowDelegate {
    private static final String BASE_STORAGE = "/data/data/";
    private Context mContext;
    private IndexUpdateDao mIndexUpdateDao;
    private TempDao mTempDao;

    public IndexFlowDelegate(Context context) {
        this.mContext = context;
        mIndexUpdateDao = HtCodePushDb.getDb(context).indexUpdateDao();
        mTempDao = HtCodePushDb.getDb(context).tempDao();
    }

    /**
     * 加载bundle成功后，调用该函数，用于判断热更新的成功、失败，以判断是否回滚操作
     */
    @Override
    public void loadBundleSuccess() {
       long tempUpdateId = getTempUpdateId();
        if (tempUpdateId > 0) {
            IndexUpdateEntity[] updateEntities = mIndexUpdateDao.findById(tempUpdateId);
            for (IndexUpdateEntity updateEntity : updateEntities) {
                updateEntity.setFail(false);
                updateEntity.setTemp(false);
            }
            mIndexUpdateDao.updateEntities(updateEntities);
        }
    }

    @Override
    public String getJsBundleFile() {
        // 查询更新表bundle版本，不包括逻辑删除
        IndexUpdateEntity[] updateEntities = mIndexUpdateDao.findByIsFailOrIsTemp(false, true);
        if (updateEntities.length > 0 && FileHelper.isExists(updateEntities[0].getBundleFile())) {
            // 判断App版本号是否一致，热更新包是依赖于App版本号的
            String localVersionName = Utils.getVersionName(mContext);
            if (!localVersionName.equals(updateEntities[0].getVersionName())) {
                return null;
            }
            // 复位temp标志位
            for (IndexUpdateEntity updateEntity : updateEntities) {
                updateEntity.setTemp(false);
            }
            mIndexUpdateDao.updateEntities(updateEntities);
            // 用于记录该次的id，当js调用loadSuccess的时候可以找到它
            setTempUpdateId(updateEntities[0].getId());

            return updateEntities[0].getBundleFile();
        }
        return null;
    }

    @Override
    public boolean checkForHotUpdate(int versionCode) throws Throwable {
        try {
            String localVersionName = Utils.getVersionName(mContext);
            // 热更新是依赖于App版本号的
            IndexUpdateEntity[] updateEntities = mIndexUpdateDao.findByVersionName(localVersionName);
            if (updateEntities.length > 0) {
                if (!FileHelper.isExists(updateEntities[0].getBundleFile())) {
                    return true;
                }
                return versionCode > updateEntities[0].getVersionCode();
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
                String destFileDir = BASE_STORAGE + mContext.getPackageName() + "/bundles/" + versionCode;
                downloadUtil.download(url, destFileDir, null, new DownloadUtil.OnDownloadListener() {
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
                        // 2. 解压
                        // 3. 更新【更新表】
                        // boolean md5Equal = Md5Util.checkMd5(file, md5);
                        // if (!md5Equal) {
                        //     if (onDownloadListener != null) {
                        //         onDownloadListener.onFail(new Exception("文件md5不一致"));
                        //     }
                        //     return;
                        // }
                        FileHelper.unzip(file.getPath(), file.getParent());
                        String bundlePath = file.getParent() + "/bundle-android/index/index.android.bundle";
                        saveUpdateTable(url, bundlePath, versionCode, md5);
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
    private void saveUpdateTable(String downloadUrl, String bundlePath, int versionCode, String md5) {
        mIndexUpdateDao.deleteByVersionCode(versionCode);
        IndexUpdateEntity updateEntity = new IndexUpdateEntity();
        updateEntity.setBundleFile(bundlePath);
        updateEntity.setVersionCode(versionCode);
        updateEntity.setMd5(md5);
        updateEntity.setCreateAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        updateEntity.setDownloadUrl(downloadUrl);
        updateEntity.setFail(true); // 添加到表的时候，默认为失败，当js端调用loadSuccess后，再将fail置为false，temp置为false
        updateEntity.setTemp(true); // 临时标志位temp，加载后就置为false
        updateEntity.setVersionName(Utils.getVersionName(mContext));
        mIndexUpdateDao.insertEntities(updateEntity);
    }

    /**
     ******************** KV存储 ********************
     */
    private void setTempUpdateId(long updateId) {
        String key = "TempUpdateId";
        TempEntity[] tempEntities = mTempDao.findByKey(key);
        if (tempEntities.length > 0) {
            tempEntities[0].setUpdateId(updateId);
            mTempDao.updateTempEntities(tempEntities[0]);
            return;
        }
        TempEntity saveTemp = new TempEntity();
        saveTemp.setKey(key);
        saveTemp.setUpdateId(updateId);
        mTempDao.insertTempEntities(saveTemp);
    }

    private long getTempUpdateId() {
        String key = "TempUpdateId";
        TempEntity[] tempEntities = mTempDao.findByKey(key);
        if (tempEntities.length > 0) {
            return tempEntities[0].getUpdateId();
        }
        return -1;
    }

}
