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

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.slf4j.Logger;

import ch.sourcepond.io.checksum.ChecksumBuilder;
import ch.sourcepond.io.checksum.ChecksumException;
import ch.sourcepond.io.checksum.UpdatableChecksum;
import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent;
import ch.sourcepond.io.fileobserver.ResourceFilter;

/**
 * @author rolandhauser
 *
 */
class EventDispatcher {
	private static final Logger LOG = getLogger(EventDispatcher.class);
	private final ConcurrentMap<Path, UpdatableChecksum<Path>> checksums = new ConcurrentHashMap<>();
	private final Executor listenerExecutor;
	private final ChecksumBuilder checksumBuilder;
	private final ListenerRegistry registry;
	private final ListenerTaskFactory taskFactory;

	/**
	 * @param pChecksumFactory
	 * @param pRegistry
	 */
	EventDispatcher(final Executor pListenerExecutor, final ChecksumBuilder pChecksumBuilder,
			final ListenerRegistry pRegistry, final ListenerTaskFactory pTaskFactory) {
		listenerExecutor = pListenerExecutor;
		checksumBuilder = pChecksumBuilder;
		registry = pRegistry;
		taskFactory = pTaskFactory;
	}

	/**
	 * @param pAbsolutePath
	 * @param pEventType
	 */
	void fireResourceChangeEvent(final WorkspaceDirectory pDirectory, final Path pContext,
			final ResourceEvent.Type pEventType) {
		final Path absolutePath = pDirectory.toAbsolutePath(pContext);
		final Path relativePath = pDirectory.relativize(absolutePath);
		final ResourceEvent event = new ResourceEvent(absolutePath, relativePath, pEventType);
		for (final ResourceFilter filter : registry.getFilters()) {
			for (final ResourceChangeListener listener : registry.getListeners(filter)) {
				fireResourceChangeEvent(filter, listener, event);
			}
		}
	}

	/**
	 * @param pFilter
	 * @param pListener
	 * @param pDirectory
	 * @param pContext
	 * @param pEventType
	 */
	void fireResourceChangeEvent(final ResourceFilter pFilter, final ResourceChangeListener pListener,
			final WorkspaceDirectory pDirectory, final Path pContext, final ResourceEvent.Type pEventType) {
		final Path absolutePath = pDirectory.toAbsolutePath(pContext);
		final Path relativePath = pDirectory.relativize(absolutePath);
		fireResourceChangeEvent(pFilter, pListener, new ResourceEvent(absolutePath, relativePath, pEventType));
	}

	/**
	 * @param pPath
	 * @return
	 * @throws ChecksumException
	 */
	private UpdatableChecksum<Path> getChecksum(final Path pPath) throws ChecksumException {
		UpdatableChecksum<Path> checksum = checksums.get(pPath);
		if (checksum == null) {
			checksum = checksumBuilder.create(pPath);
			if (checksums.putIfAbsent(pPath, checksum) != null) {
				checksum.cancel();
			}
		} else {
			checksum.update();
		}
		return checksum;
	}

	/**
	 * @param pAbsoluteFile
	 * @return
	 */
	private boolean hasContentChanged(final Path pAbsoluteFile) {
		try {
			return getChecksum(pAbsoluteFile).equalsPrevious();
		} catch (final ChecksumException e) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Event discarded because an exception has occurred!", e);
			}
		}
		return false;
	}

	/**
	 * @param pFilter
	 * @param pListener
	 * @param pEvent
	 */
	private void fireResourceChangeEvent(final ResourceFilter pFilter, final ResourceChangeListener pListener,
			final ResourceEvent pEvent) {
		if (pFilter.isDispatched(pEvent)) {
			final Path absolutePath = pEvent.getSource();
			if (isDirectory(absolutePath) || (isRegularFile(absolutePath) && hasContentChanged(absolutePath))) {
				for (final ResourceChangeListener listener : registry.getListeners(pFilter)) {
					listenerExecutor.execute(taskFactory.newTask(listener, pEvent));
				}
			}
		}
	}
}
