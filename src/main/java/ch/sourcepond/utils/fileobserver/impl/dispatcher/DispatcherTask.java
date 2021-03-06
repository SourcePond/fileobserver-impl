/*Copyright (C) 2015 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.utils.fileobserver.impl.dispatcher;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
final class DispatcherTask implements Runnable {
	private static final Logger LOG = getLogger(DispatcherTask.class);
	private final ResourceChangeListener listener;
	private final ResourceEvent event;

	/**
	 * @param pListener
	 * @param pEvent
	 */
	DispatcherTask(final ResourceChangeListener pListener, final ResourceEvent pEvent) {
		listener = pListener;
		event = pEvent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
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
