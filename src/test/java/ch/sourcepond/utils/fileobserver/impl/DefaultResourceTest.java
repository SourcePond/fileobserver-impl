package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_ADDED;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.file.Path;
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
	private final Path storagePath = mock(Path.class);
	private final ResourceChangeListener listener = mock(ResourceChangeListener.class);
	private final CloseObserver<DefaultResource> observer = mock(CloseObserver.class);
	private final Runnable listenerTask = mock(Runnable.class);
	private URL originContent;
	private DefaultResource resource;

	/**
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		originContent = new URL("file:///anyResource");
		resource = new DefaultResource(executor, taskFactory, originContent, storagePath, observer);
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
	 * 
	 */
	@Test
	public void verifyAddListener() {
		when(taskFactory.newObserverTask(Mockito.eq(listener), event(LISTENER_ADDED))).thenReturn(listenerTask);
		resource.addListener(listener);
		final InOrder order = inOrder(taskFactory, executor);
		order.verify(taskFactory).newObserverTask(Mockito.eq(listener), event(LISTENER_ADDED));
		order.verify(executor).execute(listenerTask);
		verifyNoMoreInteractions(taskFactory, executor, storagePath);
	}
}
