/*Copyright (C) 2015 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.utils.fileobserver.integrationtest;

import static ch.sourcepond.utils.fileobserver.Constants.TEST_FILE_NAME;
import static ch.sourcepond.utils.fileobserver.Constants.WORKSPACE;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_ADDED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_REMOVED;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;
import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.WorkspaceFactory;

/**
 * @author rolandhauser
 *
 */
public abstract class WatchManagerITCase {

	/**
	 * 
	 */
	private static final TestAction NOOP = new TestAction() {

		@Override
		public void test() throws Exception {
			// noop
		}
	};

	private static final String KEY = "key";
	private static final String ADDED_KEY = "addedKey";
	private static final String ADDED_VALUE = "This content has been added after listener registration";
	private final ExecutorService asynListenerExecutor = newCachedThreadPool();
	private final TestListener listener = new TestListener();
	private URL originContent;
	private Workspace watcher;
	private Resource resource;

	/**
	 * @throws IOException
	 * 
	 */
	@Before
	public void setup() throws Exception {
		originContent = getClass().getResource("/" + TEST_FILE_NAME);
		watcher = verifyAndGetManager().create(asynListenerExecutor, WORKSPACE.getFileSystem(), WORKSPACE.toString());
		resource = watcher.copy(originContent, TEST_FILE_NAME);

		// Fixes test-run on MacOSX because WatchService is not ready when the
		// test actually starts.
		sleep(1500);

		resource.addListener(listener);
	}

	protected Workspace getWatcher() {
		return watcher;
	}

	protected abstract WorkspaceFactory verifyAndGetManager();

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
				assertEquals(LISTENER_ADDED, listener.getEvent().getType());
				assertEquals(1, listener.getProperties().size());
				assertEquals("This is the initial value", listener.getProperties().getProperty(KEY));
				assertNull(listener.getException());
			}
		});
		resource.removeListener(listener);
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				// The listener should have been informed about the fact that it
				// has
				// been added to the resource.
				assertEquals(LISTENER_REMOVED, listener.getEvent().getType());
				assertNull(listener.getException());
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
				assertEquals(Type.RESOURCE_MODIFIED, listener.getEvent().getType());
				assertEquals(2, listener.getProperties().size());
				assertEquals(ADDED_VALUE, listener.getProperties().getProperty(ADDED_KEY));
				assertNull(listener.getException());
			}
		});

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void verifyResourceChangeFileReCreated() throws Exception {
		listener.awaitAction(NOOP);
		listener.getProperties().clear();
		delete(WORKSPACE.resolve(TEST_FILE_NAME));
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				assertEquals(Type.RESOURCE_DELETED, listener.getEvent().getType());
				assertEquals(originContent, listener.getEvent().getSource().getOriginContent());
				assertNotNull(listener.getException());
				assertTrue(listener.getException() instanceof NoSuchFileException);
			}
		});
		addLine(ADDED_KEY, ADDED_VALUE);
		listener.awaitAction(new TestAction() {

			@Override
			public void test() throws Exception {
				assertEquals(Type.RESOURCE_CREATED, listener.getEvent().getType());
				assertEquals(1, listener.getProperties().size());
				assertEquals(ADDED_VALUE, listener.getProperties().getProperty(ADDED_KEY));
			}
		});
	}

}
