package xyz.mijaljevic.task;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Functional helper class for file related operations.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
final class TaskHelper
{
	/**
	 * The algorithm used for file hashing.
	 */
	public static final String FILE_HASH_ALGO = "SHA-256";

	/**
	 * Creates a {@link String} hash from the provided file. Uses the algorithm
	 * specified by the <i>FILE_HASH_ALGO</i> variable.
	 * 
	 * @param file A {@link File} to hash
	 * 
	 * @return Returns the {@link String} <i>FILE_HASH_ALGO</i> hash of the provided
	 *         file.
	 * 
	 * @throws IOException              in case it failed to read the data of the
	 *                                  provided file.
	 * @throws NoSuchAlgorithmException in case it failed to find the algorithm
	 *                                  specified by the <i>FILE_HASH_ALGO</i>
	 *                                  variable.
	 */
	public static final String hashFile(File file) throws IOException, NoSuchAlgorithmException
	{
		byte[] data = Files.readAllBytes(file.toPath());

		byte[] hash = MessageDigest.getInstance(FILE_HASH_ALGO).digest(data);

		return new BigInteger(1, hash).toString(16);
	}
}
