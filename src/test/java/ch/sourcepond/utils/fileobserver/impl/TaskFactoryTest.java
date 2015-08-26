package ch.sourcepond.utils.fileobserver.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;

/**
 * @author rolandhauser
 *
 */
public class TaskFactoryTest {
	private final Resource resource = mock(Resource.class);
	private final ResourceChangeListener listener = mock(ResourceChangeListener.class);
	private final ResourceEvent event = new ResourceEvent(resource, Type.RESOURCE_CREATED);
	private final Runnable task = new TaskFactory().newObserverTask(listener, event);

	/**
	 * 
	 */
	@Test
	public void verifyRun() {
		task.run();
		verify(listener).resourceChange(event);
	}

	/**
	 * 
	 */
	@Test
	public void caughtUnexpectedException() {
		final RuntimeException expected = new RuntimeException();
		doThrow(expected).when(listener).resourceChange(event);

		// This should not cause an exception to be thrown.
		task.run();
	}
}
