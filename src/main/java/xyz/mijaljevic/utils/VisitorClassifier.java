package xyz.mijaljevic.utils;

import java.util.regex.Pattern;

import xyz.mijaljevic.domain.entity.VisitorType;

/**
 * Classifies an incoming request as {@link VisitorType#HUMAN},
 * {@link VisitorType#AI_BOT} or {@link VisitorType#CRAWLER} based on its
 * <i>User-Agent</i> header.
 *
 * <p>
 * The classification "is best" effort and relies on matching well known
 * substrings published by AI companies and search/SEO crawlers in their
 * <i>User-Agent</i> strings. Anything that does not match either pattern is
 * considered {@link VisitorType#HUMAN}.
 * </p>
 */
public final class VisitorClassifier {
    private VisitorClassifier() {
    }

    /**
     * {@link Pattern} matching <i>User-Agent</i> strings of bots used by AI
     * companies, either to crawl training data or to fetch pages on behalf of
     * an AI assistant/agent.
     */
    private static final Pattern AI_BOT_PATTERN = Pattern.compile(
            "GPTBot|ChatGPT-User|OAI-SearchBot|ClaudeBot|Claude-Web|claude-user|anthropic-ai"
                    + "|CCBot|Google-Extended|GoogleOther|PerplexityBot|Perplexity-User"
                    + "|Bytespider|Amazonbot|Applebot-Extended|Diffbot|YouBot|Meta-ExternalAgent"
                    + "|Meta-ExternalFetcher|cohere-ai|cohere-training-data-crawler|Timpibot|ImagesiftBot"
                    + "|Webzio-Extended|Bard|DuckAssistBot",
            Pattern.CASE_INSENSITIVE);

    /**
     * {@link Pattern} matching <i>User-Agent</i> strings of well known
     * (non AI) crawlers, such as search engine indexers, social media link
     * preview fetchers and SEO tools.
     */
    private static final Pattern CRAWLER_PATTERN = Pattern.compile(
            "Googlebot|bingbot|Slurp|DuckDuckBot|Baiduspider|YandexBot|Sogou|Exabot"
                    + "|facebookexternalhit|Twitterbot|LinkedInBot|WhatsApp|Applebot|ia_archiver"
                    + "|SemrushBot|AhrefsBot|MJ12bot|DotBot|PetalBot|SeznamBot|archive.org_bot"
                    + "|UptimeRobot|Pingdom|crawler|spider|bot",
            Pattern.CASE_INSENSITIVE);

    /**
     * Classifies the supplied <i>User-Agent</i> header value.
     *
     * @param userAgent The <i>User-Agent</i> header value, may be
     *                  {@code null} or blank.
     * @return The {@link VisitorType} that best matches the supplied
     *         <i>User-Agent</i> value.
     */
    public static VisitorType classify(final String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return VisitorType.CRAWLER;
        }

        if (AI_BOT_PATTERN.matcher(userAgent).find()) {
            return VisitorType.AI_BOT;
        }

        if (CRAWLER_PATTERN.matcher(userAgent).find()) {
            return VisitorType.CRAWLER;
        }

        return VisitorType.HUMAN;
    }
}
