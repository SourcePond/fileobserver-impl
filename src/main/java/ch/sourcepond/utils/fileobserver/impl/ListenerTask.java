package ch.sourcepond.utils.fileobserver.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent;

public class ListenerTask implements Runnable {
	private static final Logger LOG = getLogger(ListenerTask.class);
	private final ResourceChangeListener listener;
	private final ResourceEvent event;

	ListenerTask(final ResourceChangeListener pListener, final ResourceEvent pEvent) {
		listener = pListener;
		event = pEvent;
	}

	@Override
	public void run() {
		try {
			listener.resourceChange(event);
		} catch (final Exception e) {
			if (LOG.isWarnEnabled()) {
				LOG.warn(e.getMessage(), e);
			}
		}
	}
}
