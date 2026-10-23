package android.os;

/** JVM-only tracing adapter. Compose's diagnostics have no rendering or platform work to trace here. */
public final class Trace {
    private Trace() {}
    public static void beginSection(String name) {}
    public static void endSection() {}
}
