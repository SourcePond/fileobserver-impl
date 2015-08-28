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

import static java.nio.file.Files.newInputStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.commons.BaseResource;
import ch.sourcepond.utils.fileobserver.commons.CloseState;
import ch.sourcepond.utils.fileobserver.commons.TaskFactory;

/**
 * @author rolandhauser
 *
 */
class DefaultResource extends BaseResource implements Closeable {
	private static final Logger LOG = getLogger(DefaultResource.class);
	private final Path storagePath;

	/**
	 * @param pOrigin
	 */
	DefaultResource(final ExecutorService pAsynListenerExecutor, final TaskFactory pTaskFactory,
			final URL pOriginalContent, final Path pStoragePath, final CloseState pState) {
		super(pOriginalContent, pTaskFactory, pAsynListenerExecutor, pState);
		storagePath = pStoragePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#open()
	 */
	@Override
	protected InputStream doOpen() throws IOException {
		return newInputStream(getStoragePath());
	}

	/**
	 * @param pType
	 */
	void informListeners(final ResourceEvent.Type pType) {
		synchronized (getListeners()) {
			if (getState().isClosed()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("listeners will not be informed because this resource is closed");
				}
			} else {
				final ResourceEvent event = new ResourceEvent(this, pType);
				for (final ResourceChangeListener listener : getListeners()) {
					fireEvent(listener, event);
				}
			}
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.commons.BaseResource#getLog()
	 */
	@Override
	protected Logger getLog() {
		return LOG;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.commons.BaseResource#doesExist()
	 */
	@Override
	protected boolean doesExist() {
		return Files.exists(getStoragePath());
	}
}
