import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Uses JUnit 5 jars supplied by Eclipse; no Maven/Gradle download required. */
public class TestLauncher {
    public static void main(String[] args) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("com.holub.life.patterns"))
                .build(), listener);
        listener.getSummary().printTo(new PrintWriter(System.out, true));
        listener.getSummary().printFailuresTo(new PrintWriter(System.out, true));
        System.exit(listener.getSummary().getTestsFoundCount() == 0
                || listener.getSummary().getTotalFailureCount() > 0 ? 1 : 0);
    }
}
