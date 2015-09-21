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

import static ch.sourcepond.io.fileobserver.ResourceEvent.Type.RESOURCE_CREATED;
import static ch.sourcepond.io.fileobserver.ResourceEvent.Type.RESOURCE_DELETED;
import static ch.sourcepond.io.fileobserver.ResourceEvent.Type.RESOURCE_MODIFIED;
import static ch.sourcepond.io.fileobserver.ResourceFilter.DISPATCH_ALL;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceEvent.Type;
import ch.sourcepond.io.fileobserver.ResourceFilter;
import ch.sourcepond.io.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.impl.dispatcher.EventDispatcher;
import ch.sourcepond.utils.fileobserver.impl.listener.ListenerRegistry;
import ch.sourcepond.utils.fileobserver.impl.replay.EventReplayFactory;

/**
 * Default implementation of the {@link Workspace} interface.
 *
 */
class DefaultWorkspace extends SimpleFileVisitor<Path>implements Workspace, Runnable {
	private static final Logger LOG = getLogger(DefaultWorkspace.class);
	private final WorkspaceDirectory directory;
	private final EventReplayFactory replayFactory;
	private final EventDispatcher dispatcher;
	private final ListenerRegistry registry;
	private final WatchService watchService;
	private volatile boolean closed;

	public DefaultWorkspace(final WorkspaceDirectory pDirectory, final EventReplayFactory pReplayFactory,
			final EventDispatcher pDispatcher, final ListenerRegistry pRegistry) throws IOException {
		directory = pDirectory;
		replayFactory = pReplayFactory;
		dispatcher = pDispatcher;
		registry = pRegistry;
		watchService = pDirectory.newWatchService();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		if (!closed) {
			closed = true;
			try {
				watchService.close();
			} catch (final IOException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * 
	 */
	private void checkNotClosed() {
		if (closed) {
			throw new IllegalStateException("Workspace closed!");
		}
	}

	@Override
	public void addListener(final ResourceChangeListener pListener) {
		checkNotClosed();
		addListener(pListener, DISPATCH_ALL);
	}

	@Override
	public void addListener(final ResourceChangeListener pListener, final ResourceFilter pFilter) {
		checkNotClosed();
		if (registry.addListener(pFilter, pListener)) {
			final FileVisitor<Path> replay = replayFactory.newReplay(directory, pFilter, pListener);
			try {
				walkFileTree(directory.getPath(), replay);
			} catch (final IOException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public void removeListener(final ResourceChangeListener pListener) {
		if (closed) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Workspace closed - doing nothing");
			}
		} else {
			registry.removeListener(pListener);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.io.fileobserver.Workspace#copy(java.io.InputStream,
	 * java.lang.String[])
	 */
	@Override
	public void copy(final InputStream pOriginContent, final String... pPath)
			throws FileAlreadyExistsException, IOException {
		copy(pOriginContent, true, pPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.io.fileobserver.Workspace#copy(java.io.InputStream,
	 * boolean, java.lang.String[])
	 */
	@Override
	public void copy(final InputStream pOriginContent, final boolean pReplaceExisting, final String... pPath)
			throws FileAlreadyExistsException, IOException {
		checkNotClosed();

		if (pReplaceExisting) {
			Files.copy(pOriginContent, directory.resolve(pPath), REPLACE_EXISTING);
		} else {
			Files.copy(pOriginContent, directory.resolve(pPath));
		}
	}

	/**
	 * @param pKind
	 * @return
	 */
	private Type getTypeOrNull(final Kind<?> pKind) {
		final Type type;
		if (ENTRY_CREATE.equals(pKind)) {
			type = RESOURCE_CREATED;
		} else if (ENTRY_MODIFY.equals(pKind)) {
			type = RESOURCE_MODIFIED;
		} else if (ENTRY_DELETE.equals(pKind)) {
			type = RESOURCE_DELETED;
		} else {
			type = null;
		}
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			while (!closed) {
				final WatchKey key = watchService.take();

				for (final WatchEvent<?> event : key.pollEvents()) {
					final Kind<?> kind = event.kind();

					// Overflow event.
					if (OVERFLOW.equals(kind)) {
						continue; // loop
					}

					final Type typeOrNull = getTypeOrNull(kind);
					if (typeOrNull != null) {
						final Path context = (Path) event.context();

						// If a new directory has been created, we have to
						// traverse it to
						// a) register the watch-service on sub-directories
						// b) fire RESOURCE_CREATED events for contained
						// files/sub-directories
						if (isDirectory(context) && RESOURCE_CREATED.equals(typeOrNull)) {
							try {
								walkFileTree(context, this);
							} catch (final IOException e) {
								if (LOG.isWarnEnabled()) {
									LOG.warn("Exception occurred while registering sub-directory " + context, e);
								}
							}
						} else {
							dispatcher.fireResourceChangeEvent(directory, context, RESOURCE_CREATED);
						}
					}
				}

				// Reset the key or mark the current thread as interrupted.
				if (!key.reset()) {
					close();
					break;
				}
			}
		} catch (final ClosedWatchServiceException | InterruptedException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(e.getMessage(), e);
			}
			close();
		}
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
		dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		dispatcher.fireResourceChangeEvent(directory, dir, RESOURCE_CREATED);
		return super.preVisitDirectory(dir, attrs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
	 * java.nio.file.attribute.BasicFileAttributes)
	 */
	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
		dispatcher.fireResourceChangeEvent(directory, file, RESOURCE_CREATED);
		return super.visitFile(file, attrs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
	}
}
