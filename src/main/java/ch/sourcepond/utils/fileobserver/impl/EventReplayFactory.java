package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceFilter;

public class EventReplayFactory {
	private final EventDispatcher dispatcher;

	EventReplayFactory(final EventDispatcher pDispatcher) {
		dispatcher = pDispatcher;
	}

	public EventReplay newReplay(final WorkspaceDirectory pDirectory, final ResourceFilter pFilter,
			final ResourceChangeListener pListener) {
		return new EventReplay(pDirectory, dispatcher, pFilter, pListener);
	}
}
