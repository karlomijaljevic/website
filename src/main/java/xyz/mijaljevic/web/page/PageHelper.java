package xyz.mijaljevic.web.page;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.ws.rs.core.HttpHeaders;
import xyz.mijaljevic.task.WatchBlogsTask;

/**
 * Functional class containing helper methods and variables associated with the
 * page package.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
public final class PageHelper
{
	private static final String HASH_ALGORITHM = "SHA-256";

	/**
	 * HTTP <i>Last-Modified</i> date format.
	 */
	private static final DateTimeFormatter LM_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

	/**
	 * HTTP <i>Last-Modified</i> time zone.
	 */
	private static final String LM_ZONE = "UTC";

	/**
	 * HTTP <i>Last-Modified</i> header. Updated periodically when a blog gets
	 * updated or created by the {@link WatchBlogsTask} class. Used by classes/pages
	 * which need be updated when a new blog is added e.g. {@link HomePage}.
	 */
	private static AtomicReference<String> LastModified = new AtomicReference<String>(
			PageHelper.parseLastModifiedTime(LocalDateTime.now()));

	/**
	 * HTTP <i>ETag</i> header. Updated periodically when a blog gets updated or
	 * created by the {@link WatchBlogsTask} class. Used by classes/pages which need
	 * be updated when a new blog is added e.g. {@link HomePage}.
	 */
	private static AtomicReference<String> ETag = new AtomicReference<String>(
			PageHelper.generateEtagHash(Instant.now().toString()));

	/**
	 * <p>
	 * Generates an <i>ETag</i> hash from the provided string.
	 * </p>
	 * <p>
	 * In case the method fails to find the currently specified hashing algorithm
	 * the method will call the {@link Quarkus} <i>asyncExit()</i> method shutting
	 * the application down.
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
			digest = MessageDigest.getInstance(HASH_ALGORITHM);
		}
		catch (NoSuchAlgorithmException e)
		{
			Log.error("Failed to find '" + HASH_ALGORITHM + "' during ETag hash creation! Shutting down!");

			Log.error(e);

			Quarkus.asyncExit(2);
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
		return lastModifiedTime.atZone(ZoneId.of(LM_ZONE)).format(LM_FORMAT);
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
		ETag.set(PageHelper.generateEtagHash(Instant.now().toString()));
		LastModified.set(PageHelper.parseLastModifiedTime(LocalDateTime.now()));
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
