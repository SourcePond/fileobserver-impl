package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
public class ListenerTaskFactory {

	public ListenerTask newTask(final ResourceChangeListener pListener, final ResourceEvent pEvent) {
		return new ListenerTask(pListener, pEvent);
	}
}
