package com.metalbeetle.fruitbat.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;
import static org.junit.Assert.*;

public class CryptoTest {
	@Test
	public void testBlockEncrypt() throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException {
		Random r = new Random();
		int[] sizes = {0, 1, 8, 16, 17, 15, 31, 33, 100, r.nextInt(1000)};
		String[] pwds = {"", "jamcat"};
		for (int size : sizes) { for (String pwd : pwds) {
			byte[] plain = new byte[size];
			r.nextBytes(plain);
			byte[] cipher = Crypto.encrypt(plain, pwd);
			byte[] plain2 = Crypto.decrypt(cipher, pwd);
			for (int i = 0; i < size; i++) {
				assertEquals(size + " bytes with password " + pwd, plain[i], plain2[i]);
			}
		} }
	}

	@Test
	public void testStreamEncrypt() throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		Random r = new Random();
		int[] sizes = {0, 1, 8, 16, 17, 15, 31, 33, 100, r.nextInt(1000)};
		String[] pwds = {"", "jamcat"};
		for (int size : sizes) { for (String pwd : pwds) {
			byte[] plain = new byte[size];
			r.nextBytes(plain);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Crypto.EncryptingOutputStream cos = new Crypto.EncryptingOutputStream(os, pwd);
			cos.write(plain);
			cos.close();
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			Crypto.DecryptingInputStream cis = new Crypto.DecryptingInputStream(is, pwd);
			byte[] plain2 = new byte[size];
			assertEquals(size, cis.read(plain2));
			assertEquals(-1, cis.read());
			for (int i = 0; i < size; i++) {
				assertEquals(size + " bytes with password " + pwd, plain[i], plain2[i]);
			}
		} }
	}

	@Test
	public void testByteByByteStreamDecrypt() throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		Random r = new Random();
		int[] sizes = {0, 1, 8, 16, 17, 15, 31, 33, 100, r.nextInt(1000)};
		String[] pwds = {"", "jamcat"};
		for (int size : sizes) { for (String pwd : pwds) {
			byte[] plain = new byte[size];
			r.nextBytes(plain);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Crypto.EncryptingOutputStream cos = new Crypto.EncryptingOutputStream(os, pwd);
			cos.write(plain);
			cos.close();
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			Crypto.DecryptingInputStream cis = new Crypto.DecryptingInputStream(is, pwd);
			for (int i = 0; i < size; i++) {
				assertEquals(size + " bytes with password " + pwd + " at position " + i, plain[i], cis.read());
			}
		} }
	}

	@Test
	public void testInputStreamEncrypt() throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		Random r = new Random();
		int[] sizes = {0, 1, 8, 16, 17, 15, 31, 33, 100, r.nextInt(1000)};
		String[] pwds = {"", "jamcat"};
		for (int size : sizes) { for (String pwd : pwds) {
			byte[] plain = new byte[size];
			r.nextBytes(plain);
			ByteArrayInputStream bis = new ByteArrayInputStream(plain);
			Crypto.EncryptingInputStream eis = new Crypto.EncryptingInputStream(bis, pwd);
			Crypto.DecryptingInputStream cis = new Crypto.DecryptingInputStream(eis, pwd);
			byte[] plain2 = new byte[size];
			assertEquals(size, cis.read(plain2));
			assertEquals(-1, cis.read());
			for (int i = 0; i < size; i++) {
				assertEquals(size + " bytes with password " + pwd, plain[i], plain2[i]);
			}
		} }
	}

	@Test
	public void testInputStreamEncrypt2() throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		Random r = new Random();
		int[] sizes = {0, 1, 8, 16, 17, 15, 31, 33, 100, r.nextInt(1000)};
		String[] pwds = {"", "jamcat"};
		for (int size : sizes) { for (String pwd : pwds) {
			byte[] plain = new byte[size];
			r.nextBytes(plain);
			ByteArrayInputStream bis = new ByteArrayInputStream(plain);
			Crypto.EncryptingInputStream eis = new Crypto.EncryptingInputStream(bis, pwd);
			byte[] cipher = new byte[size + 256];
			int l = eis.read(cipher);
			assertEquals(-1, eis.read());
			byte[] cipher2 = new byte[l];
			System.arraycopy(cipher, 0, cipher2, 0, l);
			byte[] plain2 = Crypto.decrypt(cipher2, pwd);
			for (int i = 0; i < size; i++) {
				assertEquals(size + " bytes with password " + pwd, plain[i], plain2[i]);
			}
		} }
	}

	@Test
	public void testEncryptedLength() throws UnsupportedEncodingException, IOException {
		Random r = new Random();
		int[] sizes = {0, 1, 8, 16, 17, 15, 31, 33, 100, r.nextInt(1000)};
		String[] pwds = {"", "jamcat"};
		for (int size : sizes) { for (String pwd : pwds) {
			byte[] plain = new byte[size];
			r.nextBytes(plain);
			ByteArrayInputStream bis = new ByteArrayInputStream(plain);
			Crypto.EncryptingInputStream eis = new Crypto.EncryptingInputStream(bis, pwd);
			byte[] cipher = new byte[size + 256];
			assertEquals("encrypted length with plaintext length of " + size, eis.computeEncryptedLength(plain.length), eis.read(cipher));
		}}
	}
}