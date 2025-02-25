package xyz.mijaljevic.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.HttpHeaders;
import xyz.mijaljevic.ExitCodes;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.web.page.HomePage;

/**
 * Functional class containing helper methods and variables associated with the
 * web package.
 */
public final class WebHelper
{
	/**
	 * HTTP <i>Last-Modified</i> date format.
	 */
	private static final DateTimeFormatter LM_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

	/**
	 * HTTP <i>Last-Modified</i> header. Updated periodically when a blog gets
	 * updated or created. Used by classes/pages which need be updated when a new
	 * blog is added e.g. {@link HomePage}.
	 */
	private static AtomicReference<String> LastModified = new AtomicReference<String>(
			WebHelper.parseLastModifiedTime(LocalDateTime.now()));

	/**
	 * HTTP <i>ETag</i> header. Updated periodically when a blog gets updated or
	 * created. Used by classes/pages which need be updated when a new blog is added
	 * e.g. {@link HomePage}.
	 */
	private static AtomicReference<String> ETag = new AtomicReference<String>(
			WebHelper.generateEtagHash(Instant.now().toString()));

	/**
	 * <p>
	 * Generates an <i>ETag</i> hash from the provided string.
	 * </p>
	 * 
	 * @param string A {@link String} to turn into an ETag hash.
	 * 
	 * @return Returns a hexadecimal hash from the provided string.
	 */
	public static final String generateEtagHash(String string)
	{
		MessageDigest digest = null;

		try
		{
			digest = MessageDigest.getInstance(Website.HASH_ALGORITHM);
		}
		catch (NoSuchAlgorithmException e)
		{
			Log.fatal(e);

			ExitCodes.HASH_ALGORITHM_MISSING.logAndExit();
		}

		byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));

		StringBuilder hexString = new StringBuilder(2 * hash.length);

		for (int i = 0; i < hash.length; i++)
		{
			String hex = Integer.toHexString(0xff & hash[i]);

			if (hex.length() == 1)
			{
				hexString.append('0');
			}

			hexString.append(hex);
		}

		return hexString.toString();
	}

	/**
	 * Converts the provided {@link LocalDateTime} into a HTTP <i>Last-Modified</i>
	 * header value.
	 * 
	 * @param lastModifiedTime A {@link LocalDateTime} instance.
	 * 
	 * @return An HTTP <i>Last-Modified</i> header {@link String}.
	 */
	public static final String parseLastModifiedTime(LocalDateTime lastModifiedTime)
	{
		return lastModifiedTime.atZone(Website.TIME_ZONE).format(LM_FORMAT);
	}

	/**
	 * <p>
	 * Compares the <i>Etag</i> header with the <i>If-None-Match</i> header and the
	 * <i>Last-Modified</i> header with the <i>If-Modified-Since</i> header.
	 * </p>
	 * <p>
	 * If there have been changes between the provided headers and the ones in the
	 * provided {@link HttpHeaders} instance the method returns true. The priority
	 * is given to the <i>Etag</i> and <i>If-None-Match</i> comparison.
	 * </p>
	 * 
	 * @param httpHeaders  The {@link HttpHeaders} from the client/request context.
	 * @param etag         The HTTP ETag header that the page is currently serving.
	 * @param lastModified The HTTP Last-Modified that the page is currently
	 *                     serving.
	 * 
	 * @return If there have been changes between the provided headers and the ones
	 *         in the provided {@link HttpHeaders} instance the method returns true.
	 */
	public static final boolean hasResourceChanged(HttpHeaders httpHeaders, String etag, String lastModified)
	{
		String ifNoneMatch = httpHeaders.getHeaderString(HttpHeaders.IF_NONE_MATCH);
		String ifModifiedSince = httpHeaders.getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);

		if (ifNoneMatch != null)
		{
			return !ifNoneMatch.equals(etag);
		}
		else if (ifModifiedSince != null)
		{
			return !ifModifiedSince.equals(lastModified);
		}
		else
		{
			return true;
		}
	}

	/**
	 * Updates the <i>Etag</i> and <i>Last-Modified</i> headers used by pages which
	 * monitor new blogs.
	 */
	public static final void updateCacheControlHeaders()
	{
		ETag.set(WebHelper.generateEtagHash(Instant.now().toString()));
		LastModified.set(WebHelper.parseLastModifiedTime(LocalDateTime.now()));
	}

	/**
	 * @return Returns opaquely the atomic reference of the <i>Last-Modified</i>
	 *         header.
	 */
	public static final String getLastModified()
	{
		return LastModified.getOpaque();
	}

	/**
	 * @return Returns opaquely the atomic reference of the <i>ETag</i> header.
	 */
	public static final String getETag()
	{
		return ETag.getOpaque();
	}
}
