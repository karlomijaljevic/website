package xyz.mijaljevic.web;

/**
 * Holds used web keys for the Qute templating engine. These often used keys
 * are for example the HTML head key <i>title</i>.
 */
public final class WebKeys {
    /**
     * Key/name of the Qute parameter which holds the title of the page.
     */
    public static final String TITLE = "title";

    /**
     * Key/name of the Qute parameter which usually holds the displayed blogs
     * or blog links list.
     */
    public static final String BLOGS = "blogs";

    /**
     * Key/name of the Qute parameter which usually holds a singular blog or
     * blog link.
     */
    public static final String BLOG = "blog";

    /**
     * Key/name of the Qute parameter which usually holds a blog topic.
     */
    public static final String TOPIC = "topic";

    /**
     * Key/name of the Qute parameter which usually holds a response status
     * that needs to be displayed.
     */
    public static final String STATUS = "status";
}
