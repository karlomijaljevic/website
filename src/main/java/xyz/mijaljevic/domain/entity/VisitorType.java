package xyz.mijaljevic.domain.entity;

/**
 * Classification of a visitor based on the <i>User-Agent</i> header of their
 * request.
 */
public enum VisitorType {
    /**
     * A request that does not match any known crawler or AI bot signature and
     * is therefore assumed to originate from a human using a browser.
     */
    HUMAN,
    /**
     * A request originating from a known AI bot, i.e. a crawler used to
     * gather training data or to answer prompts on behalf of an AI assistant.
     */
    AI_BOT,
    /**
     * A request originating from a known (non AI) crawler, such as a search
     * engine indexer or an SEO tool.
     */
    CRAWLER
}
