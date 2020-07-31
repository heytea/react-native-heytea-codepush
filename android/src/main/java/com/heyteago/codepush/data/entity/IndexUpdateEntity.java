package com.heyteago.codepush.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "index_update")
public class IndexUpdateEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "bundle_file")
    private String bundleFile; // bundle文件路径
    private String md5;
    @ColumnInfo(name = "version_code")
    private int versionCode; // 版本号
    @ColumnInfo(name = "version_name")
    private String versionName; // 版本名称
    @ColumnInfo(name = "create_at")
    private String createAt;
    @ColumnInfo(name = "is_delete")
    private boolean isDelete = false; // 逻辑删除
    @ColumnInfo(name = "is_fail")
    private boolean isFail = false; // 更新失败
    @ColumnInfo(name = "is_temp")
    private boolean isTemp; // 是否临时，
    @ColumnInfo(name = "download_url")
    private String downloadUrl; // bundle下载链接

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBundleFile() {
        return bundleFile;
    }

    public void setBundleFile(String bundleFile) {
        this.bundleFile = bundleFile;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    public boolean isFail() {
        return isFail;
    }

    public void setFail(boolean fail) {
        isFail = fail;
    }

    public boolean isTemp() {
        return isTemp;
    }

    public void setTemp(boolean temp) {
        isTemp = temp;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }


    @Override
    public String toString() {
        return "IndexUpdateEntity{" +
                "id=" + id +
                ", bundleFile='" + bundleFile + '\'' +
                ", md5='" + md5 + '\'' +
                ", versionCode=" + versionCode +
                ", createAt='" + createAt + '\'' +
                ", isDelete=" + isDelete +
                ", isFail=" + isFail +
                ", isTemp=" + isTemp +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
