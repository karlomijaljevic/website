package xyz.mijaljevic.lifecycle;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.logging.Log;
import io.quarkus.qute.TemplateGlobal;
import jakarta.annotation.Nullable;
import xyz.mijaljevic.domain.dto.VisitorCount;

/**
 * Handles global template data such as the visitors count.
 */
public final class GlobalTemplateData {
    private GlobalTemplateData() {
    }

    /**
     * Exposes the current visitor counts to every template under the
     * {@code visitors} key.
     *
     * @return A snapshot of the current {@link VisitorCount}.
     */
    @Nullable
    @TemplateGlobal
    @SuppressWarnings("unused")
    static VisitorCount visitors() {
        try (final InstanceHandle<VisitorCounter> instance = Arc.container().instance(VisitorCounter.class)) {
            return instance.get().snapshot();
        } catch (final Exception e) {
            Log.errorf("Cannot get visitor count!", e);
            return null;
        }
    }
}
