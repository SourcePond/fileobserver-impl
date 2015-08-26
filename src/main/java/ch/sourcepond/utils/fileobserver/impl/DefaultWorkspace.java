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

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_CREATED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_DELETED;
import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_MODIFIED;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;
import ch.sourcepond.utils.fileobserver.Workspace;

/**
 * @author rolandhauser
 *
 */
final class DefaultWorkspace implements Workspace, Runnable, CloseObserver<DefaultResource> {
	private static final Logger LOG = getLogger(DefaultWorkspace.class);
	private final Map<URL, DefaultResource> watchedFiles = new HashMap<>();
	private final Map<Path, DefaultResource> resources = new ConcurrentHashMap<>();
	private final Runtime runtime;
	private final Thread watcherThread;
	private final Thread shutdownHook;
	private final Path workspace;
	private final TaskFactory taskFactory;
	private final ExecutorService informObserverExector;
	private final WatchService watchService;
	private final CloseObserver<DefaultWorkspace> closeObserver;

	/**
	 * @param pWorkspace
	 * @param pWatchService
	 * @throws IOException
	 */
	DefaultWorkspace(final Runtime pRuntime, final Path pWorkspace, final TaskFactory pTaskFactory,
			final ExecutorService pInformObserverExector, final CloseObserver<DefaultWorkspace> pCallback)
					throws IOException {
		runtime = pRuntime;
		workspace = pWorkspace;
		taskFactory = pTaskFactory;
		informObserverExector = pInformObserverExector;
		closeObserver = pCallback;
		watchService = pWorkspace.getFileSystem().newWatchService();
		watcherThread = new Thread(this, getClass().getSimpleName() + ": " + workspace.toAbsolutePath());
		watcherThread.start();
		shutdownHook = taskFactory.newShutdownHook(this);
		runtime.addShutdownHook(shutdownHook);
	}

	/**
	 * @param pUrl
	 * @param pPath
	 * @return
	 */
	private Path determinePath(final URL pUrl, final String[] pPath) {
		Path currentPath = workspace;
		for (final String path : pPath) {
			currentPath = currentPath.resolve(path);
		}
		return currentPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.impl.WatchManager#watchFile(java.net
	 * .URL, java.lang.String[])
	 */
	@Override
	public Resource watchFile(final URL pOriginContent, final String... pPath) throws IOException {
		return watchFile(pOriginContent, true, pPath);
	}

	/**
	 * 
	 */
	private void checkClosed() {
		if (watcherThread.isInterrupted()) {
			throw new IllegalStateException("This watcher has been closed!");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.Workspace#watchFile(java.net.URL,
	 * boolean, java.lang.String[])
	 */
	@Override
	public Resource watchFile(final URL pOriginContent, final boolean pReplaceExisting, final String... pPath)
			throws IOException {
		notNull(pOriginContent, "URL cannot be null!");
		notEmpty(pPath, "At least one path element must be specified!");

		synchronized (watchedFiles) {
			checkClosed();
			DefaultResource file = watchedFiles.get(pOriginContent);

			if (file == null) {
				final Path path = determinePath(pOriginContent, pPath);
				createDirectories(path.getParent());

				try (final InputStream in = pOriginContent.openStream()) {
					if (pReplaceExisting) {
						copy(in, path, REPLACE_EXISTING);
					} else {
						copy(in, path);
					}
				}

				workspace.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
				file = new DefaultResource(informObserverExector, taskFactory, pOriginContent, path, this);

				watchedFiles.put(pOriginContent, file);
				resources.put(path.toAbsolutePath(), file);
			}
			return file;
		}
	}

	@Override
	public void closed(final DefaultResource pSource) {
		synchronized (watchedFiles) {
			watchedFiles.remove(pSource.getOriginContent());
		}
		resources.remove(pSource.getStoragePath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		try {
			synchronized (watchedFiles) {
				for (final DefaultResource rs : watchedFiles.values()) {
					rs.close();
				}
				watchedFiles.clear();
			}
			watchService.close();
		} catch (final IOException e) {
			LOG.debug(e.getMessage(), e);
		} finally {
			if (!watcherThread.isInterrupted()) {
				watcherThread.interrupt();
			}
			try {
				closeObserver.closed(this);
			} catch (final Exception e) {
				LOG.debug(e.getMessage(), e);
			} finally {
				try {
					runtime.removeShutdownHook(shutdownHook);
				} catch (final Exception e) {
					// Can happen if the vm is already shutting down i.e. close
					// has been called from the shutdown hook.
					LOG.debug(e.getMessage(), e);
				}
				resources.clear();
			}
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
			while (!watcherThread.isInterrupted()) {
				final WatchKey key = watchService.take();

				for (final WatchEvent<?> event : key.pollEvents()) {
					final Kind<?> kind = event.kind();

					// Overflow event.
					if (OVERFLOW.equals(kind)) {
						continue; // loop
					}

					final Type typeOrNull = getTypeOrNull(kind);
					if (typeOrNull != null) {
						informObservers(workspace.resolve((Path) event.context()), typeOrNull);
					}

					if (key.reset()) {
						break;
					}
				}
			}
		} catch (final InterruptedException e) {
			LOG.error(e.getMessage(), e);
		} catch (final ClosedWatchServiceException e) {
			LOG.debug(e.getMessage(), e);
		} finally {
			close();
		}
	}

	/**
	 * @param pPath
	 * @param pType
	 */
	private void informObservers(final Path pAbsolutePath, final ResourceEvent.Type pEventType) {
		final DefaultResource resource = resources.get(pAbsolutePath.toAbsolutePath());
		if (resource != null) {
			resource.informListeners(pEventType);
		}
	}
}
