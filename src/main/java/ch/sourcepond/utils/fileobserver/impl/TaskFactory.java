package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.Workspace;

/**
 * @author rolandhauser
 *
 */
class TaskFactory {

	/**
	 * @param pWorkspace
	 * @return
	 */
	Thread newShutdownHook(final Workspace pWorkspace) {
		return new WorkspaceShutdownHook(pWorkspace);
	}

	/**
	 * @param pObserver
	 * @param pResource
	 * @return
	 */
	Runnable newObserverTask(final ResourceChangeListener pObserver, final ResourceEvent pEvent,
			final DefaultResource pResource) {
		return new InformObserverTask(pObserver, pEvent, pResource);
	}
}
