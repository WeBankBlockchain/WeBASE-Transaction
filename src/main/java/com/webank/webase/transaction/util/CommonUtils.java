/*
 * Copyright 2014-2020 the original author or authors.
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

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.crypto.signature.ECDSASignatureResult;
import org.fisco.bcos.sdk.v3.crypto.signature.SM2SignatureResult;
import org.fisco.bcos.sdk.v3.crypto.signature.SignatureResult;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.utils.Numeric;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * CommonUtils.
 * 
 */
@Slf4j
public class CommonUtils {

	public static final int PUBLIC_KEY_LENGTH_64 = 64;
	public static final int HASH_LENGTH_64 = 64;
	/**
	 * unZipFiles.
	 * 
	 * @param zipFile
	 *            file
	 * @param path
	 *            path
	 */
	public static void unZipFiles(MultipartFile zipFile, String path) throws IOException, BaseException {
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

	private static String cleanString(String str) {
		if (str == null) {
			return null;
		}
		String cleanString = "";
		for (int i = 0; i < str.length(); ++i) {
			cleanString += cleanChar(str.charAt(i));
		}
		return cleanString;
	}

	private static char cleanChar(char value) {
		// 0 - 9
		for (int i = 48; i < 58; ++i) {
			if (value == i) {
				return (char) i;
			}
		}
		// 'A' - 'Z'
		for (int i = 65; i < 91; ++i) {
			if (value == i) {
				return (char) i;
			}
		}
		// 'a' - 'z'
		for (int i = 97; i < 123; ++i) {
			if (value == i) {
				return (char) i;
			}
		}
		// other valid characters
		switch (value) {
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
		default:
			return ' ';
		}
	}

	/**
	 * close Closeable.
	 * 
	 * @param closeable
	 *            object
	 */
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
	 * stringToSignatureData. 19/12/24 support guomi： add byte[] pub in signatureData
	 * byte array: [v + r + s + pub]
	 * 2021/08/05 webase-sign <=1.4.3, v=27 >=1.5.0, v=0
	 * if using web3sdk, Signature's v default 27, if using java-sdk, SignatureResult's v default 0, and add 27 in RLP encode
	 * @param signatureData signatureData
	 * @return
	 */
	public static SignatureResult stringToSignatureData(String signatureData, int encryptType) {
		byte[] byteArr = Numeric.hexStringToByteArray(signatureData);
		// 从1开始，因为此处webase-sign返回的byteArr第0位是v
		byte signV = byteArr[0];
		byte[] signR = new byte[32];
		System.arraycopy(byteArr, 1, signR, 0, signR.length);
		byte[] signS = new byte[32];
		System.arraycopy(byteArr, 1 + signR.length, signS, 0, signS.length);
		if (encryptType == CryptoType.SM_TYPE) {
			byte[] pub = new byte[64];
			System.arraycopy(byteArr, 1 + signR.length + signS.length, pub, 0, pub.length);
			return new SM2SignatureResult(pub, signR, signS);
		} else {
			return new ECDSASignatureResult(signV, signR, signS);
		}
	}

	/**
	 * signatureDataToString. 19/12/24 support guomi： add byte[] pub in signatureData
	 * @param signatureData signatureData
	 */

	public static String signatureDataToString(SM2SignatureResult signatureData) {
		byte[] byteArr;
		byteArr = new byte[1 + signatureData.getR().length + signatureData.getS().length
				+ PUBLIC_KEY_LENGTH_64];
		// v
		byteArr[0] = 0;
		// r s
		System.arraycopy(signatureData.getR(), 0, byteArr, 1, signatureData.getR().length);
		System.arraycopy(signatureData.getS(), 0, byteArr, signatureData.getR().length + 1,
				signatureData.getS().length);
		System.arraycopy(signatureData.getPub(), 0, byteArr,
				signatureData.getS().length + signatureData.getR().length + 1,
				signatureData.getPub().length);

		return Numeric.toHexString(byteArr, 0, byteArr.length, false);
	}

	public static String signatureDataToString(ECDSASignatureResult signatureData) {
		byte[] byteArr;
		byteArr = new byte[1 + signatureData.getR().length + signatureData.getS().length];
		byteArr[0] = signatureData.getV();
		System.arraycopy(signatureData.getR(), 0, byteArr, 1, signatureData.getR().length);
		System.arraycopy(signatureData.getS(), 0, byteArr, signatureData.getR().length + 1,
				signatureData.getS().length);
		return Numeric.toHexString(byteArr, 0, byteArr.length, false);
	}

	/**
	 * Object to JavaBean.
	 * 
	 * @param obj
	 *            obj
	 * @param clazz
	 *            clazz
	 * @return
	 */
	public static <T> T object2JavaBean(Object obj, Class<T> clazz) {
		if (obj == null || clazz == null) {
			log.warn("Object2JavaBean. obj or clazz null");
			return null;
		}
		String jsonStr = JsonUtils.toJSONString(obj);
		return JsonUtils.toJavaObject(jsonStr, clazz);
	}

	/**
	 * delete single File.
	 * 
	 * @param filePath
	 *            filePath
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
	 * @param path
	 *            path
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

	public static Long getWorkerId() {
		InetAddress address;
		try {
			address = InetAddress.getLocalHost();
		} catch (final UnknownHostException e) {
			log.warn("Cannot get LocalHost InetAddress, please check your network!");
			return (long) new Random().nextInt(100);
		}
		log.info("getWorkerId. address:{}", address);
		byte[] ipAddressByteArray = address.getAddress();
		Long workerId = Long.valueOf(((ipAddressByteArray[ipAddressByteArray.length - 2] & 0B11) << Byte.SIZE)
				+ (ipAddressByteArray[ipAddressByteArray.length - 1] & 0xFF));
		return workerId;
	}
}
