package xyz.mijaljevic.web.page;

/**
 * Holds used page keys for the Qute templating engine. These often used keys
 * are for example the HTML head key <i>title</i>.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
public final class PageKeys
{
	/**
	 * Key/name of the Qute parameter which holds the title of the page.
	 */
	public static final String TITLE = "title";

	/**
	 * Key/name of the Qute parameter which usually holds the displayed blogs or
	 * blog links list.
	 */
	public static final String BLOGS = "blogs";

	/**
	 * Key/name of the Qute parameter which usually holds a singular blog or blog
	 * link.
	 */
	public static final String BLOG = "blog";

	/**
	 * Key/name of the Qute parameter which usually holds a response status that
	 * needs to be displayed.
	 */
	public static final String STATUS = "status";
}