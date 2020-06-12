package com.heyteago.codepush.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "index_version")
public class IndexVersionEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "bundle_file")
    private String bundleFile; // bundle文件路径
    private String md5;
    @ColumnInfo(name = "version_code")
    private int versionCode;
    @ColumnInfo(name = "create_at")
    private String createAt;
    private boolean enable = true; // 是否启用，如果都没有启用，则会去使用 assets 中的bundle

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

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public String toString() {
        return "IndexVersionEntity{" +
                "id=" + id +
                ", bundleFile='" + bundleFile + '\'' +
                ", md5='" + md5 + '\'' +
                ", versionCode=" + versionCode +
                ", createAt='" + createAt + '\'' +
                ", enable=" + enable +
                '}';
    }
}
