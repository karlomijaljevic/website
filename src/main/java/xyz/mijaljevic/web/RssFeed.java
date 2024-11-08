package xyz.mijaljevic.web;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import xyz.mijaljevic.ExitCodes;
import xyz.mijaljevic.orm.model.Rss;
import xyz.mijaljevic.task.WatchBlogsTask;
import xyz.mijaljevic.web.page.PageHelper;

/**
 * The rss feed server. Serves the RSS feed.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@PermitAll
@Path("/rss")
public final class RssFeed
{
	@ConfigProperty(name = "application.rss-feed", defaultValue = "rss.xml")
	private String rssFilePath;

	@ConfigProperty(name = "application.cache-control")
	private String cacheControl;

	@Inject
	private HttpHeaders httpHeaders;

	/**
	 * The date time format specified by the RSS 2.0 specification (RFC 822).
	 */
	public static final DateTimeFormatter RSS_SPEC_FORMAT = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z");

	/**
	 * The default last build date for the RSS FEED. It is set to the UNIX epoch.
	 */
	public static final String DEFAULT_LAST_BUILD_DATE = "Thu, 01 Jan 1970 00:00:00 UTC";

	/**
	 * {@link JAXBContext} tied to the {@link Rss} class.
	 */
	public static final JAXBContext RSS_JAXB_CONTEXT = getRssContext();

	/**
	 * RSS FEED XML file served by the GET request to this endpoint. Updated by the
	 * {@link WatchBlogsTask}.
	 */
	private static AtomicReference<Rss> RssFeed = new AtomicReference<Rss>(null);

	@GET
	@NonBlocking
	@Produces(MediaType.TEXT_XML)
	public Response getRss()
	{
		String rssFeed = fetchRssFeed();

		String etag = String.valueOf(rssFeed.hashCode());

		String lastBuildDate = RssFeed.get().getChannel().getLastBuildDate();

		if (lastBuildDate == null || lastBuildDate.isBlank())
		{
			lastBuildDate = DEFAULT_LAST_BUILD_DATE;
		}

		String lastModified = PageHelper.parseLastModifiedTime(LocalDateTime.parse(lastBuildDate, RSS_SPEC_FORMAT));

		if (!PageHelper.hasResourceChanged(httpHeaders, etag, lastModified))
		{
			return Response.status(Status.NOT_MODIFIED).build();
		}

		return Response.ok()
				.entity(rssFeed)
				.header(HttpHeaders.ETAG, etag)
				.header(HttpHeaders.CACHE_CONTROL, cacheControl)
				.header(HttpHeaders.LAST_MODIFIED, lastModified)
				.build();
	}

	/**
	 * Updates the RSS feed served by the <i>/rss.xml</i> endpoint.
	 * 
	 * @param rss A new {@link Rss} instance to serve.
	 */
	public static final void updateRssFeed(Rss rss)
	{
		RssFeed.set(rss);
	}

	/**
	 * @return The current {@link Rss} instance served by the RSS FEED page.
	 */
	public static final Rss getRssFeed()
	{
		return RssFeed.get();
	}

	/**
	 * @return Returns the {@link JAXBContext} tied to the {@link Rss} class.
	 */
	private static final JAXBContext getRssContext()
	{
		JAXBContext context = null;

		try
		{
			context = JAXBContext.newInstance(Rss.class);
		}
		catch (JAXBException e)
		{
			Log.fatal(e);

			ExitCodes.RSS_JAXB_CONTEXT_INIT_FAILED.logAndExit();
		}

		return context;
	}

	/**
	 * Transforms the static local {@link Rss} field <i>RssFeed</i> into a XML
	 * {@link String}.
	 * 
	 * @return A {@link String} holding the RSS feed or null in case an exception
	 *         occurs.
	 */
	private static final String fetchRssFeed()
	{
		try
		{
			StringWriter stringWriter = new StringWriter();
			Marshaller marshaller = RSS_JAXB_CONTEXT.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(RssFeed.get(), stringWriter);

			return stringWriter.toString();
		}
		catch (JAXBException e)
		{
			Log.error("Failed to marshal the RSS instance into a String instance!", e);

			return null;
		}
	}
}
