/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.webank.webase.transaction.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

/**
 * CommonUtils.
 * 
 */
@Slf4j
public class CommonUtils {

    /**
     * unZipFiles.
     * 
     * @param zipFile file 
     * @param path path
     */
    public static void unZipFiles(MultipartFile zipFile, String path)
            throws IOException, BaseException {
        if (zipFile.isEmpty()) {
            throw new BaseException(ConstantCode.FILE_IS_EMPTY);
        }
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        String fileName = zipFile.getOriginalFilename();
        int pos = fileName.lastIndexOf(".");
        String extName = fileName.substring(pos + 1).toLowerCase();
        if (!extName.equals("zip")) {
            throw new BaseException(ConstantCode.NOT_A_ZIP_FILE);
        }
        File file = new File(path + fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        zipFile.transferTo(file);
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                if (!zipEntryName.endsWith(".sol")) {
                    continue;
                }
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = zf.getInputStream(entry);
                    String outPath = (path + zipEntryName).replaceAll("\\*", "/");
                    log.info("unZipFiles outPath:{}", cleanString(outPath));

                    outputStream = new FileOutputStream(cleanString(outPath));
                    byte[] buf1 = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf1)) > 0) {
                        outputStream.write(buf1, 0, len);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    System.out.println("unZipFiles IOException:" + e.toString());
                } finally {
                    close(outputStream);
                    close(inputStream);
                }
            }
        } catch (IOException e) {
            System.out.println("unZipFiles IOException:" + e.toString());
        } finally {
            close(zf);
        }
        if (file.exists()) {
            file.delete();
        }
    }
    
    private static String cleanString(String aString) {
        if (aString == null) return null;
        String cleanString = "";
        for (int i = 0; i < aString.length(); ++i) {
            cleanString += cleanChar(aString.charAt(i));
        }
        return cleanString;
    }

    private static char cleanChar(char aChar) {
        // 0 - 9
        for (int i = 48; i < 58; ++i) {
            if (aChar == i) return (char) i;
        }
        // 'A' - 'Z'
        for (int i = 65; i < 91; ++i) {
            if (aChar == i) return (char) i;
        }
        // 'a' - 'z'
        for (int i = 97; i < 123; ++i) {
            if (aChar == i) return (char) i;
        }
        // other valid characters
        switch (aChar) {
            case '\\':
                return '\\';
            case '/':
                return '/';
            case ':':
                return ':';
            case '.':
                return '.';
            case '-':
                return '-';
            case '_':
                return '_';
        }
        return ' ';
    }
    
    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                System.out.println("close IOException:" + e.toString());
            }
        }
    }

    /**
     * buildHeaders.
     * 
     * @return
     */
    public static HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return headers;
    }

    /**
     * stringToSignatureData.
     * 
     * @param signatureData signatureData
     * @return
     */
    public static SignatureData stringToSignatureData(String signatureData) {
        byte[] byteArr = Numeric.hexStringToByteArray(signatureData);
        byte[] signR = new byte[32];
        System.arraycopy(byteArr, 1, signR, 0, signR.length);
        byte[] signS = new byte[32];
        System.arraycopy(byteArr, 1 + signR.length, signS, 0, signS.length);
        return new SignatureData(byteArr[0], signR, signS);
    }

    /**
     * signatureDataToString.
     * 
     * @param signatureData signatureData
     * @return
     */
    public static String signatureDataToString(SignatureData signatureData) {
        byte[] byteArr = new byte[1 + signatureData.getR().length + signatureData.getS().length];
        byteArr[0] = signatureData.getV();
        System.arraycopy(signatureData.getR(), 0, byteArr, 1, signatureData.getR().length);
        System.arraycopy(signatureData.getS(), 0, byteArr, signatureData.getR().length + 1,
                signatureData.getS().length);
        return Numeric.toHexString(byteArr, 0, byteArr.length, false);
    }

    /**
     * Object to JavaBean.
     * 
     * @param obj obj
     * @param clazz clazz
     * @return
     */
    public static <T> T object2JavaBean(Object obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            log.warn("Object2JavaBean. obj or clazz null");
            return null;
        }
        String jsonStr = JSON.toJSONString(obj);
        return JSON.parseObject(jsonStr, clazz);
    }

    /**
     * delete single File.
     * 
     * @param filePath filePath
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * delete Files.
     * 
     * @param path path
     * @return
     */
    public static boolean deleteFiles(String path) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File dirFile = new File(path);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        File[] files = dirFile.listFiles();
        if (files == null) {
            return false;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            } else {
                flag = deleteFiles(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        return true;
    }
}
