package xyz.mijaljevic.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.domain.entity.VisitorType;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorClassifierTest {
    @Test
    @DisplayName("classify recognizes a regular browser User-Agent as HUMAN")
    void classify_browserUserAgent_isHuman() {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/124.0.0.0 Safari/537.36";

        assertThat(VisitorClassifier.classify(userAgent)).isEqualTo(VisitorType.HUMAN);
    }

    @Test
    @DisplayName("classify recognizes GPTBot as an AI bot")
    void classify_gptBot_isAiBot() {
        assertThat(VisitorClassifier.classify("Mozilla/5.0 (compatible; GPTBot/1.0; +https://openai.com/gptbot)"))
                .isEqualTo(VisitorType.AI_BOT);
    }

    @Test
    @DisplayName("classify recognizes ClaudeBot as an AI bot")
    void classify_claudeBot_isAiBot() {
        assertThat(VisitorClassifier.classify("Mozilla/5.0 (compatible; ClaudeBot/1.0; +claudebot@anthropic.com)"))
                .isEqualTo(VisitorType.AI_BOT);
    }

    @Test
    @DisplayName("classify recognizes Googlebot as a crawler")
    void classify_googlebot_isCrawler() {
        assertThat(VisitorClassifier.classify("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"))
                .isEqualTo(VisitorType.CRAWLER);
    }

    @Test
    @DisplayName("classify recognizes a generic *bot* User-Agent as a crawler")
    void classify_genericBot_isCrawler() {
        assertThat(VisitorClassifier.classify("SomeRandomBot/1.0")).isEqualTo(VisitorType.CRAWLER);
    }

    @Test
    @DisplayName("classify treats a missing User-Agent as a crawler")
    void classify_nullUserAgent_isCrawler() {
        assertThat(VisitorClassifier.classify(null)).isEqualTo(VisitorType.CRAWLER);
    }

    @Test
    @DisplayName("classify treats a blank User-Agent as a crawler")
    void classify_blankUserAgent_isCrawler() {
        assertThat(VisitorClassifier.classify("   ")).isEqualTo(VisitorType.CRAWLER);
    }
}
