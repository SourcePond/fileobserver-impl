package ch.sourcepond.utils.fileobserver.impl.dispatcher;

import java.nio.file.Path;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent;
import ch.sourcepond.io.fileobserver.ResourceFilter;
import ch.sourcepond.utils.fileobserver.impl.WorkspaceDirectory;

/**
 * @author rolandhauser
 *
 */
public interface EventDispatcher {

	/**
	 * @param pDirectory
	 * @param pContext
	 * @param pEventType
	 */
	void fireResourceChangeEvent(WorkspaceDirectory pDirectory, Path pContext, ResourceEvent.Type pEventType);

	/**
	 * @param pFilter
	 * @param pListener
	 * @param pDirectory
	 * @param pContext
	 * @param pEventType
	 */
	void fireResourceChangeEvent(ResourceFilter pFilter, ResourceChangeListener pListener,
			WorkspaceDirectory pDirectory, Path pContext, ResourceEvent.Type pEventType);

}
