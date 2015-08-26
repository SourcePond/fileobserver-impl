package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_ADDED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_REMOVED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ExecutorService;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;

/**
 * @author rolandhauser
 *
 */
public class DefaultResourceTest {

	private static class EventMatcher extends BaseMatcher<ResourceEvent> {
		private final Type expectedType;

		/**
		 * @param pExpectedType
		 */
		public EventMatcher(final Type pExpectedType) {
			expectedType = pExpectedType;
		}

		@Override
		public boolean matches(final Object item) {
			return expectedType.equals(((ResourceEvent) item).getType());
		}

		@Override
		public void describeTo(final Description description) {
			description.appendText(ResourceEvent.class.getSimpleName() + " (" + expectedType.name() + ")");
		}
	};

	private final ExecutorService executor = mock(ExecutorService.class);
	private final TaskFactory taskFactory = mock(TaskFactory.class);
	private final FileSystem fs = mock(FileSystem.class);
	private final FileSystemProvider provider = mock(FileSystemProvider.class);
	private final Path storagePath = mock(Path.class);
	private final InputStream in = mock(InputStream.class);
	private final ResourceChangeListener listener = mock(ResourceChangeListener.class);
	private final Runnable listenerTask = mock(Runnable.class);
	private URL originContent;
	private DefaultResource resource;

	/**
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		when(fs.provider()).thenReturn(provider);
		when(storagePath.getFileSystem()).thenReturn(fs);
		originContent = new URL("file:///anyResource");
		resource = new DefaultResource(executor, taskFactory, originContent, storagePath);
	}

	/**
	 * @param pExpectedType
	 * @return
	 */
	private ResourceEvent event(final Type pExpectedType) {
		return Mockito.argThat(new EventMatcher(pExpectedType));
	}

	/**
	 * 
	 */
	@Test
	public void verifyInitialState() {
		assertSame(originContent, resource.getOriginContent());
		assertSame(storagePath, resource.getStoragePath());
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyOpen() throws IOException {
		when(provider.newInputStream(storagePath)).thenReturn(in);
		assertSame(in, resource.open());
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalStateException.class)
	public void tryOpenWhenClosed() throws IOException {
		resource.close();
		resource.open();
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalStateException.class)
	public void tryAddListenerWhenClosed() {
		resource.close();
		resource.addListener(listener);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalStateException.class)
	public void tryAddRemoveWhenClosed() {
		resource.close();
		resource.removeListener(listener);
	}

	/**
	 * 
	 */
	@Test
	public void verifyAddRemoveListener() {
		when(taskFactory.newObserverTask(Mockito.eq(listener), event(LISTENER_ADDED))).thenReturn(listenerTask);
		resource.addListener(listener);
		final InOrder order = inOrder(taskFactory, executor);
		order.verify(taskFactory).newObserverTask(Mockito.eq(listener), event(LISTENER_ADDED));
		order.verify(executor).execute(listenerTask);

		// Registering a listener twice has no effect
		resource.addListener(listener);
		verifyNoMoreInteractions(taskFactory, executor, storagePath);

		when(taskFactory.newObserverTask(Mockito.eq(listener), event(LISTENER_REMOVED))).thenReturn(listenerTask);
		resource.removeListener(listener);
		order.verify(taskFactory).newObserverTask(Mockito.eq(listener), event(LISTENER_REMOVED));
		order.verify(executor).execute(listenerTask);

		// Removing a listener twice has no effect
		resource.removeListener(listener);
		verifyNoMoreInteractions(taskFactory, executor, storagePath);
	}

	/**
	 * 
	 */
	@Test
	public void tryInformListenerWhenClosed() {
		resource.addListener(listener);
		resource.close();
		verify(taskFactory, times(2)).newObserverTask((ResourceChangeListener) Mockito.any(),
				(ResourceEvent) Mockito.any());
		verify(executor, times(2)).execute((Runnable) Mockito.any());

		resource.informListeners(RESOURCE_CREATED);
		verifyNoMoreInteractions(taskFactory, executor);
	}

	/**
	 * 
	 */
	@Test
	public void verifyInformListeners() {
		final Runnable r1 = mock(Runnable.class);
		final ResourceChangeListener l1 = mock(ResourceChangeListener.class);
		when(taskFactory.newObserverTask(Mockito.eq(l1), event(Type.RESOURCE_CREATED))).thenReturn(r1);
		resource.addListener(l1);

		final Runnable r2 = mock(Runnable.class);
		final ResourceChangeListener l2 = mock(ResourceChangeListener.class);
		when(taskFactory.newObserverTask(Mockito.eq(l2), event(Type.RESOURCE_CREATED))).thenReturn(r2);
		resource.addListener(l2);

		resource.informListeners(RESOURCE_CREATED);
		verify(executor).execute(r1);
		verify(executor).execute(r2);
	}

	/**
	 * 
	 */
	@Test
	public void verifyClose() {
		final Runnable r1 = mock(Runnable.class);
		when(taskFactory.newObserverTask(Mockito.eq(listener), event(LISTENER_ADDED))).thenReturn(r1);
		resource.addListener(listener);
		final Runnable r2 = mock(Runnable.class);
		when(taskFactory.newObserverTask(Mockito.eq(listener), event(LISTENER_REMOVED))).thenReturn(r2);
		resource.close();

		final InOrder order = inOrder(taskFactory, executor);
		order.verify(taskFactory).newObserverTask(Mockito.eq(listener), event(LISTENER_ADDED));
		order.verify(executor).execute(r1);
		order.verify(taskFactory).newObserverTask(Mockito.eq(listener), event(LISTENER_REMOVED));
		order.verify(executor).execute(r2);

		// Further close should not have any effect
		resource.close();
		verifyNoMoreInteractions(taskFactory, executor);
	}

	/**
	 * 
	 */
	@Test
	public void verifyToString() {
		assertEquals(storagePath.toString(), resource.toString());
	}
}
