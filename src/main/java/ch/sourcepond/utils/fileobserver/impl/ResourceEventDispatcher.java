package ch.sourcepond.utils.fileobserver.impl;

import java.nio.file.Path;

import ch.sourcepond.io.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
class ResourceEventDispatcher {
	private final ListenerRegistry registry;

	ResourceEventDispatcher(final ListenerRegistry pRegistry) {
		registry = pRegistry;
	}

	/**
	 * @param pAbsolutePath
	 * @param pEventType
	 */
	void fireResourceChangeEvent(final Path pAbsolutePath, final Path pRelativePath,
			final ResourceEvent.Type pEventType, final boolean pIsDirectory) {

	}
}
