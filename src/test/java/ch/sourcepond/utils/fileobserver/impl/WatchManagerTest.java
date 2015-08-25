package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_ADDED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_REMOVED;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.delete;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;
import ch.sourcepond.utils.fileobserver.WatchManager;
import ch.sourcepond.utils.fileobserver.Watcher;
import ch.sourcepond.utils.fileobserver.impl.DefaultWatchManager;

/**
 * @author rolandhauser
 *
 */
public class WatchManagerTest {

	/**
	 * 
	 */
	private static final TestAction NOOP = new TestAction() {

		@Override
		public void test() throws Exception {
			// noop
		}
	};

	/**
	 * @author rolandhauser
	 *
	 */
	private static interface TestAction {

		/**
		 * 
		 */
		void test() throws Exception;
	}

	/**
	 * @author rolandhauser
	 *
	 */
	private class TestListener implements ResourceChangeListener {
		private final Lock lock = new ReentrantLock();
		private final Condition actionDoneCondition = lock.newCondition();
		private final Properties props = new Properties();
		private ResourceEvent event;
		private Exception unexpected;
		private boolean actionDone;

		public void awaitAction(final TestAction pTest) throws Exception {
			lock.lock();
			try {
				int c = 0;
				while (!actionDone) {
					actionDoneCondition.await(500, MILLISECONDS);

					if (c++ > 100) {
						fail("Action not executed!");
					}
				}
				pTest.test();
			} finally {
				actionDone = false;
				lock.unlock();
			}
		}

		@Override
		public void resourceChange(final ResourceEvent pEvent) {
			lock.lock();
			try (final InputStream in = pEvent.getSource().open()) {
				props.load(in);
			} catch (final IOException e) {
				unexpected = e;
			} finally {
				actionDone = true;
				event = pEvent;
				actionDoneCondition.signalAll();
				lock.unlock();
			}
		}
	}

	private static final String TEST_FILE_NAME = "test.properties";
	private static final String KEY = "key";
	private static final String ADDED_KEY = "addedKey";
	private static final String ADDED_VALUE = "This content has been added after listener registration";
	private static final Path WORKSPACE = getDefault().getPath(SystemUtils.USER_DIR, "target");
	private final ExecutorService observerInforExecutor = Executors.newCachedThreadPool();
	private final TestListener listener = new TestListener();
	private final WatchManager factory = new DefaultWatchManager();
	private URL originContent;
	private Watcher manager;
	private Resource resource;

	/**
	 * @throws IOException
	 * 
	 */
	@Before
	public void setup() throws Exception {
		originContent = getClass().getResource("/" + TEST_FILE_NAME);
		manager = factory.watch(WORKSPACE, observerInforExecutor);
		resource = manager.watchFile(originContent, TEST_FILE_NAME);

		// Fixes test-run on MacOSX because WatchService is not ready when the
		// test actually starts.
		sleep(1500);

		resource.addListener(listener);
	}

	/**
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (manager != null) {
			manager.close();
		}
	}

	/**
	 * @param pLine
	 * @throws Exception
	 */
	private void addLine(final String pKey, final String pValue) throws Exception {
		try (final Writer writer = Files.newBufferedWriter(WORKSPACE.resolve(TEST_FILE_NAME), Charset.defaultCharset(),
				APPEND, CREATE)) {
			writer.write("\n" + pKey + "=" + pValue);
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void verifyListenerAddedRemoved() throws Exception {
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				// The listener should have been informed about the fact that it
				// has
				// been added to the resource.
				assertEquals(LISTENER_ADDED, listener.event.getType());
				assertEquals(1, listener.props.size());
				assertEquals("This is the initial value", listener.props.getProperty(KEY));
			}
		});
		resource.removeListener(listener);
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				// The listener should have been informed about the fact that it
				// has
				// been added to the resource.
				assertEquals(LISTENER_REMOVED, listener.event.getType());
			}
		});
	}

	/**
	 * 
	 */
	@Test
	public void verifyResourceChangeFileModified() throws Exception {
		listener.awaitAction(NOOP);
		addLine(ADDED_KEY, ADDED_VALUE);
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				assertEquals(Type.RESOURCE_MODIFIED, listener.event.getType());
				assertEquals(2, listener.props.size());
				assertEquals(ADDED_VALUE, listener.props.getProperty(ADDED_KEY));
			}
		});

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void verifyResourceChangeFileReCreated() throws Exception {
		listener.awaitAction(NOOP);
		listener.props.clear();
		delete(WORKSPACE.resolve(TEST_FILE_NAME));
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				assertEquals(Type.RESOURCE_DELETED, listener.event.getType());
				assertEquals(originContent, listener.event.getSource().getOriginContent());
			}
		});
		addLine(ADDED_KEY, ADDED_VALUE);
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				assertEquals(Type.RESOURCE_CREATED, listener.event.getType());
				assertEquals(1, listener.props.size());
				assertEquals(ADDED_VALUE, listener.props.getProperty(ADDED_KEY));
			}
		});
	}
}
