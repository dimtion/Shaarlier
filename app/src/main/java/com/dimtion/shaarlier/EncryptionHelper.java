package com.dimtion.shaarlier;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by dimtion on 13/05/2015.
 * A simple class to encrypt and decrypt simple data
 * (Probably needs review)
 */
class EncryptionHelper {
    public final static int KEY_LENGTH = 256;
    public final static int IV_LENGTH = 16;

    public static SecretKey generateKey() throws NoSuchAlgorithmException {

        SecureRandom secureRandom = new SecureRandom();
        // Do *not* seed secureRandom! Automatically seeded from system entropy.
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(KEY_LENGTH, secureRandom);
        return keyGenerator.generateKey();
    }

    public static byte[] generateInitialVector() {
        SecureRandom random = new SecureRandom();
        return random.generateSeed(IV_LENGTH);
    }

    public static String secretKeyToString(SecretKey secretKey) {
        return Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
    }

    public static SecretKey stringToSecretKey(String stringKey) {
        byte[] encodedKey = Base64.decode(stringKey, Base64.DEFAULT);
        return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }

    private static byte[] encryptDecrypt(int mode, byte[] clear, SecretKey key, byte[] initialVector)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initialVector);

        cipher.init(mode, key, ivParameterSpec);
        return cipher.doFinal(clear);
    }

    public static byte[] encrypt(byte[] clear, SecretKey key, byte[] initialVector)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        return encryptDecrypt(Cipher.ENCRYPT_MODE, clear, key, initialVector);
    }

    public static byte[] decrypt(byte[] encrypted, SecretKey key, byte[] initialVector)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        return encryptDecrypt(Cipher.DECRYPT_MODE, encrypted, key, initialVector);
    }

    public static byte[] stringToBase64(String clear) throws UnsupportedEncodingException {
        return Base64.encode(clear.getBytes(), Base64.DEFAULT);
    }

    public static String base64ToString(byte[] data) throws UnsupportedEncodingException {
        return new String(Base64.decode(data, Base64.DEFAULT));
    }
}
