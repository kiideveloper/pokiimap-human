package com.zl.pokemap.betterpokemap.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.google.gson.Gson;
import com.pokegoapi.auth.Login;
import com.pokegoapi.exceptions.LoginFailedException;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PmGoogleLogin extends Login {

	public static final String OAUTH_ENDPOINT = "https://android.clients.google.com/auth";
	private final OkHttpClient client;
	private final Context context;

	public PmGoogleLogin(OkHttpClient client, Context context) {
		this.client = client;
		this.context = context;
	}

	/**
	 * Returns an AuthInfo object given a token, this should not be an access token but rather an id_token
	 *
//	 * @param String the id_token stored from a previous oauth attempt.
	 * @return AuthInfo a AuthInfo proto structure to be encapsulated in server requests
	 */
	public AuthInfo login(String token) {
		AuthInfo.Builder builder = AuthInfo.newBuilder();
		builder.setProvider("google");
		builder.setToken(AuthInfo.JWT.newBuilder().setContents(token).setUnknown2(59).build());
		AuthInfo auth = builder.build();

		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor ed=mPrefs.edit();
		Gson gson = new Gson();
		ed.putString("auth", gson.toJson(auth));
		ed.commit();
		return auth;
	}

	private FormBody.Builder getGoogleLoginFormBuilder(){
		return new FormBody.Builder()
				.addEncoded("accountType", "HOSTED_OR_GOOGLE")
				.addEncoded("has_permission", "1")
				.addEncoded("source", "android")
				.addEncoded("androidId", "9774d56d682e549c")
				.addEncoded("device_country", "us")
				.addEncoded("operatorCountry", "us")
				.addEncoded("lang", "en")
				.addEncoded("sdk_version", "17");
	}

	/**
	 * Starts a login flow for google using a username and password, this uses googles device oauth endpoint, a URL and code is display
	 * not really ideal right now.
	 *
//	 * @param String Google username
//	 * @param String Google password
	 * @return AuthInfo a AuthInfo proto structure to be encapsulated in server requests
	 */
	public AuthInfo login(String username, String password) throws LoginFailedException {
		try {
			HttpUrl url = HttpUrl.parse(OAUTH_ENDPOINT).newBuilder()
					.build();
			FormBody fb = getGoogleLoginFormBuilder()
					.addEncoded("Email", username)
					.addEncoded("EncryptedPasswd", encrypt(username, password))
					.addEncoded("add_account", "1") //what the heck is this?
					.build();

			Request request = new Request.Builder()
					.url(url)
					.method("POST", fb)
					.build();

			Response response = client.newCall(request).execute();

			BufferedReader reader = new BufferedReader(new StringReader(response.body().string()));
			String line = null;
			Map<String, String> res = new HashMap<>();
			while((line = reader.readLine()) != null){
				String[] pair = line.split("=");
				res.put(pair[0], pair[1]);
			}

			String accessToken = res.get("Token");
			if(accessToken == null){
				res.get("token");
			}

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("auth_provider", "google")
            .putString("token", accessToken)
            .putString("username", username)
            .commit();


			String authToken = oauth(username, accessToken);

			return login(authToken);
		} catch (Exception e) {
			throw new LoginFailedException(e.getMessage());
		}

	}

	private static final long T90MINS = 90*60*60*1000L;
	public String oauth(String username, String accessToken) throws Exception {
		HttpUrl url = HttpUrl.parse(OAUTH_ENDPOINT).newBuilder()
				.build();
		FormBody fb = getGoogleLoginFormBuilder()
				.addEncoded("Email", username)
				.addEncoded("EncryptedPasswd", accessToken)
				//secret sauce from pokemon go app, thank you Internet
				.addEncoded("service", "audience:server:client_id:848232511240-7so421jotr2609rmqakceuu1luuq0ptb.apps.googleusercontent.com")
				.addEncoded("source", "android")
				.addEncoded("androidId", "9774d56d682e549c")
				.addEncoded("app", "com.nianticlabs.pokemongo")
				.addEncoded("client_sig", "321187995bc7cdc2b5fc91b11a96e2baa8602c62")
				.build();

		Request request = new Request.Builder()
				.url(url)
				.method("POST", fb)
				.build();

		Response response = client.newCall(request).execute();

		BufferedReader reader = new BufferedReader(new StringReader(response.body().string()));
		String line = null;
		Map<String, String> res = new HashMap<>();
		while ((line = reader.readLine()) != null) {
			String[] pair = line.split("=");
			res.put(pair[0], pair[1]);
		}

		String auth = res.get("Auth");
		if (auth == null) {
			res.get("auth");
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			String expires = res.get("Expiry");
			long duration = Math.min(T90MINS, Long.parseLong(expires)); //milli seconds?
			prefs.edit().putLong("expiry", System.currentTimeMillis()+duration)
					.commit();
		}catch (Exception e){
			e.printStackTrace();
			prefs.edit().remove("expiry");
		}


		return auth;
	}


			private static final String googleDefaultPublicKey = "AAAAgMom/1a/v0lblO2Ubrt60J2gcuXSljGFQXgcyZWveWLEwo6prwgi3iJIZdodyhKZQrNWp5nKJ3srRXcUW+F1BD3baEVGcmEgqaLZUNBjm057pKRI16kB0YppeGx5qIQ5QjKzsR8ETQbKLNWgRY0QRNVz34kMJR3P/LgHax/6rmf5AAAAAwEAAQ==";

	// In:
	//   login - your mail, should looks like myemail@gmail.com
	//   password - your password
	// Out:
	//   a base64 string containing the encrypted password

	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	// WARNING!!! THE CODE WORKS CORRECTLY ONLY IF THE LENGTH OF login+password
	// IS LESS THAT 80 CHARS (YES, DO NOT CHECK IT IN THE METHOD, I'M A CRAPPY
	// JAVA CODER :))
	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

	@SuppressWarnings("static-access")
	public static String encrypt(String login, String password)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, UnsupportedEncodingException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException {

		// First of all, let's convert Google login public key from base64
		// to PublicKey, and then calculate SHA-1 of the key:

		// 1. Converting Google login public key from base64 to byte[]
		byte[] binaryKey = Base64.decode(googleDefaultPublicKey, 0);

		// 2. Calculating the first BigInteger
		int i = readInt(binaryKey, 0);
		byte [] half = new byte[i];
		System.arraycopy(binaryKey, 4, half, 0, i);
		BigInteger firstKeyInteger = new BigInteger(1, half);

		// 3. Calculating the second BigInteger
		int j = readInt(binaryKey, i + 4);
		half = new byte[j];
		System.arraycopy(binaryKey, i + 8, half, 0, j);
		BigInteger secondKeyInteger = new BigInteger(1, half);

		// 4. Let's calculate SHA-1 of the public key, and put it to signature[]:
		// signature[0] = 0 (always 0!)
		// signature[1...4] = first 4 bytes of SHA-1 of the public key
		byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(binaryKey);
		byte[] signature = new byte[5];
		signature[0] = 0;
		System.arraycopy(sha1, 0, signature, 1, 4);

		// 5. Use the BigInteger's (see calculations above) to generate
		// a PublicKey object
		PublicKey publicKey = KeyFactory.getInstance("RSA").
				generatePublic(new RSAPublicKeySpec(firstKeyInteger, secondKeyInteger));

		// It's time to encrypt our password:
		// 1. Let's create Cipher:
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING");

		// 2. Then concatenate the login and password (use "\u0000" as a separator):
		String combined = login + "\u0000" + password;

		// 3. Then converting the string to bytes
		byte[] plain = combined.getBytes("UTF-8");

		// 4. and encrypt the bytes with the public key:
		cipher.init(cipher.PUBLIC_KEY, publicKey);
		byte[] encrypted = cipher.doFinal(plain);

		// 5. Add the result to a byte array output[] of 133 bytes length:
		// output[0] = 0 (always 0!)
		// output[1...4] = first 4 bytes of SHA-1 of the public key
		// output[5...132] = encrypted login+password ("\u0000" is used as a separator)
		byte[] output = new byte [133];
		System.arraycopy(signature, 0, output, 0, signature.length);
		System.arraycopy(encrypted, 0, output, signature.length, encrypted.length);

		// Done! Just encrypt the result as base64 string and return it
		return Base64.encodeToString(output, Base64.URL_SAFE + Base64.NO_WRAP);
	}

	// Aux. method, it takes 4 bytes from a byte array and turns the bytes to int
	private static int readInt(byte[] arrayOfByte, int start) {
		return 0x0 | (0xFF & arrayOfByte[start]) << 24 | (0xFF & arrayOfByte[(start + 1)]) << 16 | (0xFF & arrayOfByte[(start + 2)]) << 8 | 0xFF & arrayOfByte[(start + 3)];
	}


}
