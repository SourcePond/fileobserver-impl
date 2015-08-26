package ch.sourcepond.utils.fileobserver.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author rolandhauser
 *
 */
public class DefaultWorkspaceFactoryTest {
	private final Runtime runtime = mock(Runtime.class);
	private final Thread watcherThread = mock(Thread.class);
	private final Thread shutdownHook = mock(Thread.class);
	private final ThreadFactory threadFactory = mock(ThreadFactory.class);
	private final TaskFactory taskFactory = mock(TaskFactory.class);
	private final FileSystem fs = mock(FileSystem.class);
	private final Path workspace = mock(Path.class);
	private final ExecutorService asynListenerExecutor = mock(ExecutorService.class);
	private final WatchService watchService = mock(WatchService.class);
	@SuppressWarnings("unchecked")
	private final CloseObserver<DefaultWorkspace> closeObserver = mock(CloseObserver.class);
	private final DefaultWorkspaceFactory factory = new DefaultWorkspaceFactory(runtime, threadFactory, taskFactory);

	/**
	 * 
	 */
	@Before
	public void setup() throws IOException {
		when(workspace.getFileSystem()).thenReturn(fs);
		when(fs.newWatchService()).thenReturn(watchService);
		when(threadFactory.newWatcher(workspaceMatcher(), Mockito.eq(workspace))).thenReturn(watcherThread);
		when(threadFactory.newShutdownHook(workspaceMatcher())).thenReturn(shutdownHook);
	}

	/**
	 * 
	 */
	@Test
	public void verifyDefaultConstructor() {
		// Should not cause an exception to be thrown
		new DefaultWorkspaceFactory();
	}

	/**
	 * @return
	 */
	private DefaultWorkspace workspaceMatcher() {
		return Mockito.argThat(new BaseMatcher<DefaultWorkspace>() {

			@Override
			public boolean matches(final Object item) {
				return item instanceof DefaultWorkspace;
			}

			@Override
			public void describeTo(final Description description) {
				description.appendText(DefaultWorkspace.class.getName());
			}
		});
	}

	private void verifyWorkspaceCreation(final DefaultWorkspace pWorkspace) {
		verify(watcherThread).start();
		verify(shutdownHook, never()).start();
		pWorkspace.close();
		verify(watcherThread).interrupt();
		verify(runtime).removeShutdownHook(shutdownHook);
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyPublicCreate() throws IOException {
		verifyWorkspaceCreation(factory.create(workspace, asynListenerExecutor));
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyCreate() throws IOException {
		final DefaultWorkspace ws = factory.create(workspace, asynListenerExecutor, closeObserver);
		verifyWorkspaceCreation(ws);
		verify(closeObserver).closed(ws);
	}

	/**
	 * 
	 */
	@Test
	public void verifyClosed() {
		// noop, should not cause an exception to be thrown.
		factory.closed(null);
	}
}
