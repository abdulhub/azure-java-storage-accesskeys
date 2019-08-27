package com.springprojects.azure.springazurestorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

	/**
	 * Author: Abdul G
	 */
	@Value("${storage.account}")
	private String account;

	@Value("${storage.account.container}")
	private String container;

	@Value("${storage.account.container.file}")
	private String fileName;

	@Value("${storage.account.key}")
	private String key;

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger();

	@GetMapping(path = "/readblob")
	public String readBlob() throws Exception {
		return readBlob(account, key, container, fileName);
	}

	@GetMapping(path = "/{account}/{container}/{blob}")
	public String readBlob(@PathVariable("account") String account, @PathVariable("container") String container,
			@PathVariable("blob") String fileName) throws Exception {

		return readBlob(account, key, container, fileName);
	}

	public String readBlob(String account, String key, String container, String fileName) throws Exception {
		String urlString = "http://" + account + ".blob.core.windows.net" + "/" + container + "/" + fileName;
		String canonicalizedResource = account + "/" + container + "/" + fileName;
		HttpURLConnection connection = null;
		connection = (HttpURLConnection) (new URL(urlString)).openConnection();
		connection = setRequestHeaders(connection, account, key, canonicalizedResource);
		connection.connect();

		String content = readResponse(connection);

		return content;

	}

	private String readResponse(HttpURLConnection connection) throws IOException {
		BufferedReader bufferedReader = null;
		if (connection.getResponseCode() != 200) {
			bufferedReader = new BufferedReader(new InputStreamReader((connection.getErrorStream())));
		} else {
			bufferedReader = new BufferedReader(new InputStreamReader((connection.getInputStream())));
		}
		return readContent(bufferedReader);
	}

	private HttpURLConnection setRequestHeaders(HttpURLConnection connection, String account, String key,
			String canonicalizedResource) throws ProtocolException, InvalidKeyException, NoSuchAlgorithmException,
			IllegalStateException, UnsupportedEncodingException {
		String date = getDate();

		String stringToSign = "GET\n" + "\n" // content encoding
				+ "\n" // content language
				+ "\n" // content length
				+ "\n" // content md5
				+ "\n" // content type
				+ "\n" // date
				+ "\n" // if modified since
				+ "\n" // if match
				+ "\n" // if none match
				+ "\n" // if unmodified since
				+ "\n" // range
				+ "x-ms-date:" + date + "\nx-ms-version:2014-02-14\n" // headers
				+ "/" + canonicalizedResource; // resources

		String auth = prepareAuthenticationString(stringToSign, account, key);

		connection.setRequestMethod("GET");
		connection.setRequestProperty("x-ms-date", date);
		connection.setRequestProperty("x-ms-version", "2014-02-14");
		connection.setRequestProperty("Authorization", auth);
		return connection;
	}

	private String getDate() {
		SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date = fmt.format(Calendar.getInstance().getTime()) + " GMT";
		return date;
	}

	private String prepareAuthenticationString(String stringToSign, String account, String key)
			throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(Base64.getDecoder().decode(key), "HmacSHA256"));
		String authKey = new String(Base64.getEncoder().encode(mac.doFinal(stringToSign.getBytes("UTF-8"))));
		String auth = "SharedKey " + account + ":" + authKey;
		return auth;
	}

	public String readContent(BufferedReader reader) {
		return reader.lines().collect(Collectors.joining(System.lineSeparator()));
	}

}
