package zw.co.zivai.core_backend.common.services.sync;

public final class SyncContext {
    private static final ThreadLocal<Boolean> OUTBOX_SUPPRESSED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SyncContext() {
    }

    public static boolean isOutboxSuppressed() {
        return OUTBOX_SUPPRESSED.get();
    }

    public static void runWithoutOutbox(Runnable runnable) {
        boolean previous = OUTBOX_SUPPRESSED.get();
        OUTBOX_SUPPRESSED.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            OUTBOX_SUPPRESSED.set(previous);
        }
    }
}
