package ch.sourcepond.utils.fileobserver.obsolete;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.obsolete.ThreadFactory;

/**
 * @author rolandhauser
 *
 */
public class ThreadFactoryTest {
	private final ThreadFactory factory = new ThreadFactory();
	private final Path workspacePath = mock(Path.class);
	private final Runnable runnable = mock(Runnable.class);
	private final Workspace workspace = mock(Workspace.class);

	/**
	 * @throws IOException
	 * 
	 */
	@Test
	public void verifyShutdownHook() throws IOException {
		final Thread hook = factory.newShutdownHook(workspace);
		hook.run();
		verify(workspace).close();
	}

	/**
	 * 
	 */
	@Test
	public void verifyNewWatcher() {
		factory.newWatcher(runnable, workspacePath).run();
		verify(runnable).run();
	}

	/**
	 * @throws IOException
	 * 
	 */
	@Test
	public void verifyShutdownHookCaughtException() throws IOException {
		final IOException expected = new IOException();
		doThrow(expected).when(workspace).close();
		final Thread hook = factory.newShutdownHook(workspace);
		// This should not cause an exception to be thrown.
		hook.run();
	}
}
