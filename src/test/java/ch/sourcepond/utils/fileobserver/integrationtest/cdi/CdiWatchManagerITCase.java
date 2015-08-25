package ch.sourcepond.utils.fileobserver.integrationtest.cdi;

import static com.google.inject.Guice.createInjector;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.After;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

import ch.sourcepond.utils.fileobserver.WatchManager;
import ch.sourcepond.utils.fileobserver.impl.DefaultWatchManager;
import ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase;

/**
 * @author rolandhauser
 *
 */
public class CdiWatchManagerITCase extends WatchManagerITCase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase#
	 * verifyAndGetManager()
	 */
	@Override
	protected WatchManager verifyAndGetManager() {
		final Injector injector = createInjector(new Module() {

			@Override
			public void configure(final Binder binder) {
				binder.bind(WatchManager.class).to(DefaultWatchManager.class);
			}
		});
		final WatchManager watchManager = injector.getInstance(WatchManager.class);
		// Verify that the watch-manager is a singleton
		assertSame(watchManager, injector.getInstance(WatchManager.class));
		return watchManager;
	}

	/**
	 * @throws IOException
	 * 
	 */
	@After
	public void tearDown() throws IOException {
		if (getWatcher() != null) {
			getWatcher().close();
		}
	}
}
