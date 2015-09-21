package ch.sourcepond.utils.fileobserver.impl;

import static org.mockito.Mockito.mock;

import ch.sourcepond.utils.fileobserver.impl.dispatcher.DefaultEventDispatcher;
import ch.sourcepond.utils.fileobserver.impl.replay.DefaultEventReplayFactory;

/**
 * @author rolandhauser
 *
 */
public class DefaultWorkspaceTest {
	private final WorkspaceDirectory dir = mock(WorkspaceDirectory.class);
	private final DefaultEventReplayFactory evenrplFactory = mock(DefaultEventReplayFactory.class);
	private final DefaultEventDispatcher dispatcher = mock(DefaultEventDispatcher.class);
}
