package com.heyteago.codepush.util;

import android.content.Context;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileHelper {
    private FileHelper() {
    }

    public static boolean isExists(String file) {
        File f = new File(file);
        return f.exists();
    }

    /**
     * 拷贝assets 目录下的文件到指定目录
     *
     * @param context
     * @param targetPath
     * @param targetFile
     * @param assetName
     */
    public static void cpAssetsToFile(Context context, String targetPath, String targetFile, String assetName) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = context.getApplicationContext().getAssets().open(assetName);
            File dir = new File(targetPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, targetFile);
            fileOutputStream = new FileOutputStream(file);
            byte[] buff = new byte[2048];
            int len = -1;
            while ((len = inputStream.read(buff)) != -1) {
                fileOutputStream.write(buff, 0, len);
            }
            inputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    /**
     * 解压 .tar.gz
     *
     * @param srcFile  目标文件
     * @param destPath 目标输出路径
     * @throws IOException
     */
    public static void UnTarGz(File srcFile, String destPath)
            throws IOException {
        byte[] buf = new byte[1024];
        FileInputStream fis = new FileInputStream(srcFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        GzipCompressorInputStream cis = new GzipCompressorInputStream(bis);
        TarArchiveInputStream tais = new TarArchiveInputStream(cis);
        TarArchiveEntry tae = null;
        int pro = 0;
        while ((tae = tais.getNextTarEntry()) != null) {
            File f = new File(destPath + "/" + tae.getName());
            if (tae.isDirectory()) {
                f.mkdirs();
            } else {
                /*
                 * 父目录不存在则创建
                 */
                File parent = f.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int len;
                while ((len = tais.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                bos.flush();
                bos.close();
            }
        }
        tais.close();
    }

    /**
     * 把zip文件解压到指定的文件夹
     *
     * @param zipFile zip文件路径, 如 "D:/test/aa.zip"
     * @param destDir 解压后的文件存放路径, 如"D:/test/"
     */
    public static void unzip(String zipFile, String destDir) {
        FileInputStream fis = null;
        ArchiveInputStream ais = null;
        FileOutputStream fos = null;
        File f;
        try {
            fis = new FileInputStream(zipFile);
            ais = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, fis);
            ArchiveEntry entry;
            while ((entry = ais.getNextEntry()) != null) {
                if (!ais.canReadEntryData(entry)) {
                    continue;
                }
                f = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                    fos = new FileOutputStream(f.getPath());
                    IOUtils.copy(ais, fos);
                }
            }
        } catch (ArchiveException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (ais != null) {
                    ais.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


//        try (ArchiveInputStream i = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, Files.newInputStream(Paths.get(zipFile)))) {
//            ArchiveEntry entry = null;
//            while ((entry = i.getNextEntry()) != null) {
//                if (!i.canReadEntryData(entry)) {
//                    continue;
//                }
//                f = new File(destDir, entry.getName());
//                if (entry.isDirectory()) {
//                    if (!f.isDirectory() && !f.mkdirs()) {
//                        throw new IOException("failed to create directory " + f);
//                    }
//                } else {
//                    File parent = f.getParentFile();
//                    if (!parent.isDirectory() && !parent.mkdirs()) {
//                        throw new IOException("failed to create directory " + parent);
//                    }
//                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
//                        IOUtils.copy(i, o);
//                    }
//                }
//            }
//        } catch (IOException | ArchiveException e) {
//            e.printStackTrace();
//        }

    }

    /**
     * 判断文件名是否以.zip为后缀
     *
     * @param fileName 需要判断的文件名
     * @return 是zip文件返回true, 否则返回false
     */
    public static boolean isEndsWithZip(String fileName) {
        boolean flag = false;
        if (fileName != null && !"".equals(fileName.trim())) {
            if (fileName.endsWith(".ZIP") || fileName.endsWith(".zip")) {
                flag = true;
            }
        }
        return flag;
    }

}
