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
package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_ADDED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.LISTENER_REMOVED;
import static java.nio.file.Files.newInputStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;

/**
 * @author rolandhauser
 *
 */
final class DefaultResource implements Resource, Closeable {
	private static final Logger LOG = getLogger(DefaultResource.class);
	private final Set<ResourceChangeListener> listeners = new HashSet<>();
	private final ExecutorService executor;
	private final TaskFactory taskFactory;
	private final URL originContent;
	private final Path storagePath;
	private final CloseObserver<DefaultResource> callback;
	private volatile boolean closed;

	/**
	 * @param pOrigin
	 */
	DefaultResource(final ExecutorService pAsynListenerExecutor, final TaskFactory pTaskFactory,
			final URL pOriginalContent, final Path pStoragePath, final CloseObserver<DefaultResource> pCallback) {
		executor = pAsynListenerExecutor;
		taskFactory = pTaskFactory;
		originContent = pOriginalContent;
		storagePath = pStoragePath;
		callback = pCallback;
	}

	/**
	 * 
	 */
	private void checkClosed() {
		if (closed) {
			throw new IllegalStateException("This watcher has been closed!");
		}
	}

	private boolean isClosed() {
		return closed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#addObserver(ch.
	 * sourcepond.utils.content.observer.ChangeObserver)
	 */
	@Override
	public void addListener(final ResourceChangeListener pObserver) {
		checkClosed();
		synchronized (listeners) {
			if (!listeners.add(pObserver)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Observer {0} already present, nothing to be added.", pObserver);
					return;
				}
			}
		}
		fireEvent(pObserver, new ResourceEvent(this, LISTENER_ADDED));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#removeObserver(ch.
	 * sourcepond.utils.content.observer.ChangeObserver)
	 */
	@Override
	public void removeListener(final ResourceChangeListener pListener) {
		if (isClosed()) {
			LOG.warn("Workspace is closed; do nothing");
			return;
		}
		synchronized (listeners) {
			if (!listeners.remove(pListener)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Observer {0} not present, nothing to be removed.", pListener);
				}
				return;
			}
		}
		fireEvent(pListener, new ResourceEvent(this, LISTENER_REMOVED));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#open()
	 */
	@Override
	public InputStream open() throws IOException {
		checkClosed();
		return newInputStream(storagePath);
	}

	/**
	 * @param pListener
	 */
	private void fireEvent(final ResourceChangeListener pListener, final ResourceEvent pEvent) {
		executor.execute(taskFactory.newObserverTask(pListener, pEvent));
	}

	/**
	 * @param pType
	 */
	void informListeners(final ResourceEvent.Type pType) {
		final ResourceEvent event = new ResourceEvent(this, pType);
		synchronized (listeners) {
			for (final ResourceChangeListener listener : listeners) {
				fireEvent(listener, event);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.Resource#getOriginContent()
	 */
	@Override
	public URL getOriginContent() {
		return originContent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.ClosableResource#doClose()
	 */
	@Override
	public void close() throws IOException {
		closed = true;
		synchronized (listeners) {
			for (final ResourceChangeListener listener : listeners) {
				fireEvent(listener, new ResourceEvent(this, Type.LISTENER_REMOVED));
			}
			listeners.clear();
		}
		callback.closed(this);
	}

	/**
	 * @return
	 */
	Path getStoragePath() {
		return storagePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getStoragePath().toString();
	}
}
