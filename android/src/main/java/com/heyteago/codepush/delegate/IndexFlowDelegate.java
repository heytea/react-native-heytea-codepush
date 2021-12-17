package com.heyteago.codepush.delegate;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.heyteago.codepush.data.HtCodePushDb;
import com.heyteago.codepush.data.dao.IndexUpdateDao;
import com.heyteago.codepush.data.dao.TempDao;
import com.heyteago.codepush.data.entity.IndexUpdateEntity;
import com.heyteago.codepush.util.DownloadUtil;
import com.heyteago.codepush.util.FileHelper;
import com.heyteago.codepush.util.Utils;
import com.heyteago.codepush.vo.BundleConfig;

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

    @Override
    public String getJsBundleFile() {
        String localVersionName = Utils.getVersionName(mContext);
        // 查询更新表bundle版本，不包括逻辑删除
        IndexUpdateEntity[] entities = mIndexUpdateDao.findByIsFailAndVersionName(false, localVersionName);
        for (IndexUpdateEntity entity : entities) {
            if (FileHelper.isExists(entity.getBundleFile())) {
                return entity.getBundleFile();
            }
        }
        return null;
    }

    @Override
    public void loadFail() {
        String localVersionName = Utils.getVersionName(mContext);
        IndexUpdateEntity[] entities = mIndexUpdateDao.findByIsFailAndVersionName(false, localVersionName);
        if (entities.length > 0) {
            IndexUpdateEntity entity = entities[0];
            entity.setFail(true);
            mIndexUpdateDao.updateEntities(entity);
        }
    }

    @Override
    public boolean checkForHotUpdate(int versionCode) throws Throwable {
        try {
            String localVersionName = Utils.getVersionName(mContext);
            // 热更新是依赖于App版本号的
            IndexUpdateEntity[] updateEntities = mIndexUpdateDao.findByVersionName(localVersionName);
            if (updateEntities.length > 0) {
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
                        String bundlePath = file.getParent() + "/bundle-android/index.android.bundle";
                        if (!new File(bundlePath).exists()) {
                            if (onDownloadListener != null) {
                                onDownloadListener.onFail(new Exception("没有找到index.android.bundle，更新失败"));
                            }
                            saveUpdateTable(url, bundlePath, versionCode, md5, true);
                            return;
                        }
                        // 读取配置信息，判断是否支持最低版本更新，如果没有这个配置文件，则直接认为更新失败
                        String configFilePath = file.getParent() + "/bundle-android/config.json";
                        String configStr = FileHelper.readFileString(configFilePath);
                        if (TextUtils.isEmpty(configStr)) {
                            if (onDownloadListener != null) {
                                onDownloadListener.onFail(new Exception("没有读取到热更包中的配置文件，更新失败"));
                            }
                            saveUpdateTable(url, bundlePath, versionCode, md5, true);
                            return;
                        }
                        Gson gson = new Gson();
                        BundleConfig bundleConfig = gson.fromJson(configStr, BundleConfig.class);
                        String localVersionName = Utils.getVersionName(mContext);
                        int diff = Utils.compareVersion(localVersionName, bundleConfig.getMinVersion());
                        if (diff < 0) {
                            if (onDownloadListener != null) {
                                onDownloadListener.onFail(new Exception("当前版本小于热更包最小支持的版本，更新失败"));
                            }
                            saveUpdateTable(url, bundlePath, versionCode, md5, true);
                            return;
                        }
                        saveUpdateTable(url, bundlePath, versionCode, md5, false);
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

    @Override
    public int getHotVersion() {
        String localVersionName = Utils.getVersionName(mContext);
        // 查询更新表bundle版本，不包括逻辑删除
        IndexUpdateEntity[] entities = mIndexUpdateDao.findByIsFailAndVersionName(false, localVersionName);
        for (IndexUpdateEntity entity : entities) {
            if (FileHelper.isExists(entity.getBundleFile())) {
                return entity.getVersionCode();
            }
        }
        return 0;
    }

    /**
     * 更新【更新表】
     */
    private void saveUpdateTable(String downloadUrl, String bundlePath, int versionCode, String md5, boolean isFail) {
        mIndexUpdateDao.deleteByVersionCode(versionCode);
        IndexUpdateEntity updateEntity = new IndexUpdateEntity();
        updateEntity.setBundleFile(bundlePath);
        updateEntity.setVersionCode(versionCode);
        updateEntity.setMd5(md5);
        updateEntity.setCreateAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        updateEntity.setDownloadUrl(downloadUrl);
        updateEntity.setFail(isFail);
        updateEntity.setVersionName(Utils.getVersionName(mContext));
        mIndexUpdateDao.insertEntities(updateEntity);
    }

}
