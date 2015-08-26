package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_CREATED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_DELETED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_MODIFIED;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;
import ch.sourcepond.utils.fileobserver.commons.CloseState;

/**
 *
 */
public class RunDefaultWorkspaceTest extends BaseDefaultWorkspaceTest {
	private final WatchKey key = mock(WatchKey.class);
	private final WatchEvent<?> event = mock(WatchEvent.class);
	private final Path context = mock(Path.class);
	private final DefaultResource resource = mock(DefaultResource.class);

	/**
	 * @throws InterruptedException
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Before
	public void setup() throws InterruptedException {
		when(state.isClosed()).thenReturn(false).thenReturn(true);
		when(key.reset()).thenReturn(true);
		when(watchService.take()).thenReturn(key);
		when((Path) event.context()).thenReturn(context);
		when((List) key.pollEvents()).thenReturn(asList(event));
		when(workspacePath.resolve(context)).thenReturn(file);
		watcherThreadCache.put(absoluteFile, resource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.impl.BaseDefaultWorkspaceTest#newState()
	 */
	@Override
	protected CloseState newState() {
		return mock(CloseState.class);
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void verifyNoListenerFound() {
		watcherThreadCache.clear();
		when((Kind) event.kind()).thenReturn(ENTRY_CREATE);
		workspace.run();
		verify(resource, Mockito.never()).informListeners((Type) Mockito.any());
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void verifyRunCreate() {
		when((Kind) event.kind()).thenReturn(ENTRY_CREATE);
		workspace.run();
		verify(resource).informListeners(RESOURCE_CREATED);
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void verifyRunModify() {
		when((Kind) event.kind()).thenReturn(ENTRY_MODIFY);
		workspace.run();
		verify(resource).informListeners(RESOURCE_MODIFIED);
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void verifyRunDelete() {
		when((Kind) event.kind()).thenReturn(ENTRY_DELETE);
		workspace.run();
		verify(resource).informListeners(RESOURCE_DELETED);
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void verifyRunUnknownEvent() {
		when((Kind) event.kind()).thenReturn(null);
		workspace.run();
		verify(resource, Mockito.never()).informListeners((Type) Mockito.any());
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void verifyRunOverflow() {
		when((Kind) event.kind()).thenReturn(OVERFLOW);
		workspace.run();
		verify(workspacePath, Mockito.never()).resolve(context);
	}

	private void verifyExceptionalClose() throws InterruptedException {
		workspace.run();

		// Should have been called exactly once because the loop is broken after
		// the first iteration.
		verify(watchService).take();
		verify(watcherThread).interrupt();
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void verifyKeyReset() throws InterruptedException {
		when(state.isClosed()).thenReturn(false).thenReturn(false).thenReturn(true);
		when(key.reset()).thenReturn(false);
		workspace.run();
		verifyExceptionalClose();
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void verifyTakeInterrupted() throws InterruptedException {
		when(state.isClosed()).thenReturn(false).thenReturn(false).thenReturn(true);
		final InterruptedException expected = new InterruptedException();
		doThrow(expected).when(watchService).take();
		verifyExceptionalClose();
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void verifyWatchServiceClosed() throws InterruptedException {
		when(state.isClosed()).thenReturn(false).thenReturn(false).thenReturn(true);
		final ClosedWatchServiceException expected = new ClosedWatchServiceException();
		doThrow(expected).when(watchService).take();
		workspace.run();
		verifyExceptionalClose();
	}
}
