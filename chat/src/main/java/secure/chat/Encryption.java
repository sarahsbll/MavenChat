package secure.chat;

import javax.crypto.*;
import javax.crypto.spec.*;

import java.security.*;
import java.security.spec.*;
import javax.crypto.KeyAgreement;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Encryption {
	private PrivateKey keyPrivate;
	private PublicKey keyPublic;

	private IvParameterSpec ivspec; // used for AES/CBC
	private byte[] iv = new byte[16]; // used for AES/CBC

	private GCMParameterSpec gcmspec; // used for AES/GCM
	private final int TAG = 128; // used for AES/GCM

	private SecretKeySpec secretKeySpec; // used for AES/CBC and AES/GCM
	private String algorithm; // used for AES/CBC and AES/GCM
	private byte[] secretBytes; // used for AES/CBC and AES/GCM

	/**
	 * Initialize the secretKey with keyBytes
	 *
	 */
	public Encryption(PrivateKey keyPrivate, PublicKey keyPublic, byte[] secretBytes, String algorithm)
			throws ErrorException {
		try {

			Security.addProvider(new BouncyCastleProvider());

			KeyPairGenerator ecKeyGen = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
			ecKeyGen.initialize(new ECGenParameterSpec("brainpoolP384r1"));

			// doesn't work, which means we are dancing on the leading edge :)
			// KeyPairGenerator ecKeyGen = KeyPairGenerator.getInstance("EC");
			// ecKeyGen.initialize(new ECGenParameterSpec("secp384r1"));

			KeyPair ecKeyPair = ecKeyGen.generateKeyPair();
			this.keyPrivate = keyPrivate;
			this.keyPublic = keyPublic;

			// different key specs initialization with secretBytes
			this.algorithm = algorithm;

			this.secretBytes = secretBytes;
			secretKeySpec = new SecretKeySpec(secretBytes, "AES");
			ivspec = new IvParameterSpec(iv); // used for AES/CBC
			gcmspec = new GCMParameterSpec(TAG, secretBytes); // used for AES/GCM

			System.out.println("PHASE 4   shared secret key: " + secretKeySpec);
		} catch (Exception e) {
			throw new ErrorException(":err FAILED TO CREATE AN ENCRYPTION OBJECT!");
		}
	}

	String message = "Hello World";

	// System.out.println(Hex.toHexString(ciphertext));

	// System.out.println(new String(plaintext));

	/**
	 * Encrypt a string with AES/GCM mode
	 *
	 * @param message is the plain text
	 * @return the encrypted cipher text
	 */
	public String encrypt(String plainText) throws ErrorException {
		try {

			// initialize IEScipher with public key
			Cipher iesCipher = Cipher.getInstance("ECIESwithAES-CBC");
			iesCipher.init(Cipher.ENCRYPT_MODE, ecKeyPair.getPublic());

			// encrypt plain text
			byte[] cipherText = iesCipher.doFinal(plainText.getBytes());
			return Base64.getEncoder().encodeToString(cipherText);

		} catch (AEADBadTagException e) {
			throw new ErrorException(":err MESSAGE INTEGRITY CHECK IN ENCRYPTION FAILED!\n");
		} catch (Exception e) {
			throw new ErrorException(":err ENCRYPTION USING " + e + " FAILED!\n");
		}
	}

	/**
	 * Decrypt a string with AES/GCM mode
	 *
	 * @param message is the cipher text
	 * @return the decrypted plain text
	 */
	public String decrypt(String cipherText) throws ErrorException {
		try {

			Cipher iesCipher = Cipher.getInstance("ECIESwithAES-CBC");
			iesCipher.init(Cipher.ENCRYPT_MODE, keyPublic);

			// initialize IEScipher with private key

			Cipher iesDecipher = Cipher.getInstance("ECIESwithAES-CBC");
			iesDecipher.init(Cipher.DECRYPT_MODE, ecKeyPair.getPrivate(), iesCipher.getParameters());

			// initialize cipher with secret key
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmspec);

			// decrypt cipher text
			byte[] decoder = Base64.getDecoder().decode(cipherText);
			byte[] plainText = iesDecipher.doFinal(decoder);

			return new String(plainText, "UTF-8");

		} catch (AEADBadTagException e) {
			throw new ErrorException(":err MESSAGE INTEGRITY CHECK IN DECRYPTION FAILED!\n");
		} catch (Exception e) {
			throw new ErrorException(":err DECRYPTION USING " + algorithm + " FAILED!\n");
		}
	}

	/**
	 * Encrypt a string with AES/CBC mode
	 *
	 * @param message is the plain text
	 * @return the encrypted cipher text
	 */
	public String CBCencrypt(String plainText) throws ErrorException {
		try {
			// initialize cipher with secret key
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);

			// encrypt plain text
			byte[] cipherText = cipher.doFinal(plainText.getBytes());
			return Base64.getEncoder().encodeToString(cipherText);

		} catch (AEADBadTagException e) {
			throw new ErrorException(":err MESSAGE INTEGRITY CHECK IN ENCRYPTION FAILED!\n");
		} catch (Exception e) {
			throw new ErrorException(":err ENCRYPTION USING " + algorithm + " FAILED!\n");
		}
	}

	/**
	 * Decrypt a string with AES/CBC mode
	 *
	 * @param message is the cipher text
	 * @return the decrypted plain text
	 */
	public String CBCdecrypt(String cipherText) throws ErrorException {
		try {
			// initialize cipher with secret key
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);

			// decrypt cipher text
			byte[] decoder = Base64.getDecoder().decode(cipherText);
			byte[] plainText = cipher.doFinal(decoder);
			return new String(plainText, "UTF-8");

		} catch (AEADBadTagException e) {
			throw new ErrorException(":err MESSAGE INTEGRITY CHECK IN DECRYPTION FAILED!\n");
		} catch (Exception e) {
			throw new ErrorException(":err DECRYPTION USING " + algorithm + " FAILED!\n");
		}
	}
}
