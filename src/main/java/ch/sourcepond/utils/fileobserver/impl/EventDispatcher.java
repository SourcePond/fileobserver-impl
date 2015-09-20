package ch.sourcepond.utils.fileobserver.impl;

import java.nio.file.Path;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent;
import ch.sourcepond.io.fileobserver.ResourceFilter;

/**
 * @author rolandhauser
 *
 */
class EventDispatcher {
	private final ListenerRegistry registry;

	EventDispatcher(final ListenerRegistry pRegistry) {
		registry = pRegistry;
	}

	/**
	 * @param pAbsolutePath
	 * @param pEventType
	 */
	void fireResourceChangeEvent(final WorkspaceDirectory pDirectory, final Path pContext,
			final ResourceEvent.Type pEventType, final boolean pIsDirectory) {

	}

	void fireResourceChangeEvent(final ResourceFilter pFilter, final ResourceChangeListener pListener,
			final WorkspaceDirectory pDirectory, final Path pContext, final ResourceEvent.Type pEventType,
			final boolean pIsDirectory) {
		final Path absolutePath = pDirectory.toAbsolutePath(pContext);
		final Path relativePath = pDirectory.relativize(absolutePath);

		final ResourceEvent event = new ResourceEvent(absolutePath, relativePath, pEventType);

	}
}
