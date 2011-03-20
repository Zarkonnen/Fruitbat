package com.metalbeetle.fruitbat.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Cryptographic functions based on BouncyCastle's crypto systems. Uses AES/CBC/SHA256.
 */
public class Crypto {
	private static final SecureRandom SR = new SecureRandom();
	private static final String SALT = "sodium chloride is delicious";

	public static byte[] encrypt(byte[] src, String pwd) throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException {
		pwd = pwd + SALT;
		PaddedBufferedBlockCipher c = new PaddedBufferedBlockCipher(
				new CBCBlockCipher(new AESEngine()));
		SHA256Digest dig = new SHA256Digest();
		byte[] key = new byte[dig.getDigestSize()];
		dig.update(pwd.getBytes("UTF-8"), 0, pwd.getBytes("UTF-8").length);
		dig.doFinal(key, 0);
		c.init(true, new KeyParameter(key));
		byte[] iv = new byte[c.getBlockSize()];
		SR.nextBytes(iv);
		byte[] dst = new byte[c.getOutputSize(src.length + iv.length)];
		int offset = c.processBytes(iv, 0, iv.length, dst, 0);
		offset += c.processBytes(src, 0, src.length, dst, offset);
		c.doFinal(dst, offset);
		return dst;
	}

	public static byte[] decrypt(byte[] src, String pwd) throws UnsupportedEncodingException, DataLengthException, IllegalStateException, InvalidCipherTextException {
		pwd = pwd + SALT;
		PaddedBufferedBlockCipher c = new PaddedBufferedBlockCipher(
				new CBCBlockCipher(new AESEngine()));
		SHA256Digest dig = new SHA256Digest();
		byte[] key = new byte[dig.getDigestSize()];
		dig.update(pwd.getBytes("UTF-8"), 0, pwd.getBytes("UTF-8").length);
		dig.doFinal(key, 0);
		c.init(false, new KeyParameter(key));
		byte[] dst = new byte[c.getOutputSize(src.length)];
		int offset = c.processBytes(src, 0, src.length, dst, 0);
		c.doFinal(dst, offset);
		byte[] dst2 = new byte[dst.length - c.getBlockSize()];
		System.arraycopy(dst, c.getBlockSize(), dst2, 0, dst2.length);
		return dst2;
	}

	/** Encrypting wrapper for input stream. */
	public static final class EncryptingInputStream extends InputStream {
		final InputStream s;
		final PaddedBufferedBlockCipher c;
		final byte[] plainBuf;
		final byte[] cipherBuf;
		final byte[] ivBlock;
		final int ivBlockLength;
		int bufOffset = 0;
		int bufLength = 0;
		int ivOffset = 0;
		boolean done = false;

		public EncryptingInputStream(InputStream s, String pwd) throws UnsupportedEncodingException {
			pwd = pwd + SALT;
			this.s = s;
			c = new PaddedBufferedBlockCipher(
					new CBCBlockCipher(new AESEngine()));
			SHA256Digest dig = new SHA256Digest();
			byte[] key = new byte[dig.getDigestSize()];
			dig.update(pwd.getBytes("UTF-8"), 0, pwd.getBytes("UTF-8").length);
			dig.doFinal(key, 0);
			c.init(true, new KeyParameter(key));
			plainBuf = new byte[c.getBlockSize()];
			cipherBuf = new byte[c.getBlockSize() * 2];
			byte[] iv = new byte[c.getBlockSize()];
			SR.nextBytes(iv);
			// Generate the IV.
			ivBlock = new byte[c.getUpdateOutputSize(iv.length) + c.getBlockSize()];
			ivBlockLength = c.processBytes(iv, 0, iv.length, ivBlock, 0);
		}

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			int length = read(b);
			return length == -1 ? -1 : b[0];
		}

		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int offset, int length) throws IOException {
			int left = length;
			// First, supply the IV.
			if (ivOffset < ivBlockLength) {
				int amountToReadFromIV = Math.min(left, ivBlockLength - ivOffset);
				System.arraycopy(ivBlock, ivOffset, b, offset, amountToReadFromIV);
				ivOffset += amountToReadFromIV;
				left -= amountToReadFromIV;
				offset += amountToReadFromIV;
			}

			// Then, supply the rest of the stream.
			while (left > 0 && bufLength != -1) {
				int readFromBuf = Math.min(left, bufLength);
				System.arraycopy(cipherBuf, bufOffset, b, offset, readFromBuf);
				bufLength -= readFromBuf;
				bufOffset += readFromBuf;
				offset += readFromBuf;
				left -= readFromBuf;
				if (bufLength == 0 && left > 0) {
					nextBlock();
				}
			}
			if (length - left == 0 && bufLength == -1) { return -1; }
			return length - left;
		}

		void nextBlock() throws IOException {
			bufOffset = 0;
			if (done) { bufLength = -1; return; }
			bufLength = s.read(plainBuf);
			if (bufLength == -1) {
				done = true;
				try {
					bufLength = c.doFinal(cipherBuf, 0);
				} catch (InvalidCipherTextException e) {
					throw new IOException("Invalid ciphertext: " + e.getMessage());
				}
			} else {
				bufLength = c.processBytes(plainBuf, 0, bufLength, cipherBuf, 0);
			}
		}

		@Override
		public void close() throws IOException {
			s.close();
		}
	}

	/** Decrypting wrapper for input stream. */
	public static final class DecryptingInputStream extends InputStream {
		final InputStream s;
		final PaddedBufferedBlockCipher c;
		final byte[] cipherBuf;
		final byte[] plainBuf;
		int bufOffset = 0;
		int bufLength = 0;
		boolean done = false;

		public DecryptingInputStream(InputStream s, String pwd) throws UnsupportedEncodingException, IOException {
			pwd = pwd + SALT;
			this.s = s;
			c = new PaddedBufferedBlockCipher(
					new CBCBlockCipher(new AESEngine()));
			SHA256Digest dig = new SHA256Digest();
			byte[] key = new byte[dig.getDigestSize()];
			dig.update(pwd.getBytes("UTF-8"), 0, pwd.getBytes("UTF-8").length);
			dig.doFinal(key, 0);
			c.init(false, new KeyParameter(key));
			cipherBuf = new byte[c.getBlockSize()];
			plainBuf = new byte[c.getBlockSize()];
			// Eat and discard the IV.
			int ivLeft = c.getBlockSize();
			byte[] discardMe = new byte[c.getBlockSize()];
			while (ivLeft > 0) {
				ivLeft -= read(discardMe, 0, ivLeft);
			}
		}

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			int length = read(b);
			return length == -1 ? -1 : b[0];
		}

		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int offset, int length) throws IOException {
			int left = length;
			while (left > 0 && bufLength != -1) {
				int readFromBuf = Math.min(left, bufLength);
				System.arraycopy(plainBuf, bufOffset, b, offset, readFromBuf);
				bufLength -= readFromBuf;
				bufOffset += readFromBuf;
				offset += readFromBuf;
				left -= readFromBuf;
				if (bufLength == 0 && left > 0) {
					nextBlock();
				}
			}
			if (length - left == 0 && bufLength == -1) { return -1; }
			return length - left;
		}

		void nextBlock() throws IOException {
			bufOffset = 0;
			if (done) { bufLength = -1; return; }
			bufLength = s.read(cipherBuf);
			if (bufLength == -1) {
				done = true;
				try {
					bufLength = c.doFinal(plainBuf, 0);
				} catch (InvalidCipherTextException e) {
					throw new IOException("Invalid ciphertext: " + e.getMessage());
				}
			} else {
				bufLength = c.processBytes(cipherBuf, 0, bufLength, plainBuf, 0);
			}
		}

		@Override
		public void close() throws IOException {
			s.close();
		}
	}

	/** Encrypting wrapper for output stream. */
	public static class EncryptingOutputStream extends OutputStream {
		final OutputStream s;
		final PaddedBufferedBlockCipher c;
		byte[] buf;

		public EncryptingOutputStream(OutputStream s, String pwd) throws UnsupportedEncodingException, IOException {
			pwd = pwd + SALT;
			this.s = s;
			c = new PaddedBufferedBlockCipher(
					new CBCBlockCipher(new AESEngine()));
			SHA256Digest dig = new SHA256Digest();
			byte[] key = new byte[dig.getDigestSize()];
			try {
				dig.update(pwd.getBytes("UTF-8"), 0, pwd.getBytes("UTF-8").length);
			} catch (UnsupportedEncodingException e) {
				throw new IOException("UTF-8 not supported!");
			}
			dig.doFinal(key, 0);
			c.init(true, new KeyParameter(key));
			byte[] iv = new byte[c.getBlockSize()];
			SR.nextBytes(iv);
			buf = new byte[c.getUpdateOutputSize(iv.length) + 16];
			int len = c.processBytes(iv, 0, iv.length, buf, 0);
			s.write(buf, 0, len);
		}

		@Override
		public void write(int b) throws IOException {
			ensureBufSize(c.getUpdateOutputSize(1));
			int offset = c.processByte((byte) b,buf, 0);
			s.write(buf, 0, offset);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			ensureBufSize(c.getUpdateOutputSize(b.length));
			int offset = c.processBytes(b, 0, len, buf, 0);
			s.write(buf, 0, offset);
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void close() throws IOException {
			try {
				ensureBufSize(c.getOutputSize(0));
				int offset = c.doFinal(buf, 0);
				s.write(buf, 0, offset);
				s.close();
			} catch (InvalidCipherTextException e) {
				throw new IOException("Invalid ciphertext: " + e.getMessage());
			}
		}

		void ensureBufSize(int size) {
			if (buf.length < size) {
				buf = new byte[size + 16];
			}
		}
	}
}
