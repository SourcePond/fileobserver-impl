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
import static org.apache.commons.lang3.StringUtils.isBlank;
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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;
import ch.sourcepond.utils.fileobserver.commons.BaseWorkspace;
import ch.sourcepond.utils.fileobserver.commons.CloseObserver;
import ch.sourcepond.utils.fileobserver.commons.CloseState;
import ch.sourcepond.utils.fileobserver.commons.TaskFactory;

/**
 * @author rolandhauser
 *
 */
class DefaultWorkspace extends BaseWorkspace implements Runnable {
	private static final Logger LOG = getLogger(DefaultWorkspace.class);
	private final ConcurrentMap<Path, DefaultResource> watcherThreadCache;
	private final Runtime runtime;
	private final Path workspace;
	private final WatchService watchService;
	private final CloseObserver closeObserver;
	private Thread watcherThread;
	private Thread shutdownHook;

	/**
	 * @param pWorkspace
	 * @param pWatchService
	 * @throws IOException
	 */
	DefaultWorkspace(final Runtime pRuntime, final CloseState pState, final Path pWorkspace,
			final TaskFactory pTaskFactory, final ExecutorService pAsynListenerExecutor,
			final CloseObserver pCloseObserver, final Map<URL, Resource> pManagedResourcesCache,
			final ConcurrentMap<Path, DefaultResource> pWatcherThreadCache) throws IOException {
		super(pManagedResourcesCache, pAsynListenerExecutor, pTaskFactory, pState);
		assert pRuntime != null;
		assert pState != null;
		assert pWorkspace != null;
		assert pTaskFactory != null;
		assert pAsynListenerExecutor != null;
		assert pCloseObserver != null;
		assert pManagedResourcesCache != null;
		assert pWatcherThreadCache != null;

		runtime = pRuntime;
		workspace = pWorkspace;
		closeObserver = pCloseObserver;
		watcherThreadCache = pWatcherThreadCache;
		watchService = pWorkspace.getFileSystem().newWatchService();
	}

	/**
	 * @param pWatcherThread
	 */
	void setWatcherThread(final Thread pWatcherThread) {
		synchronized (getManagedResourcesCache()) {
			watcherThread = pWatcherThread;
		}
	}

	/**
	 * @param pShutdownHook
	 */
	void setShutdownHook(final Thread pShutdownHook) {
		synchronized (getManagedResourcesCache()) {
			shutdownHook = pShutdownHook;
			runtime.addShutdownHook(pShutdownHook);
		}
	}

	/**
	 * @param pUrl
	 * @param pPath
	 * @return
	 */
	private Path determinePath(final URL pUrl, final String[] pPath) {
		Path currentPath = workspace;

		for (final String path : pPath) {
			if (isBlank(path)) {
				throw new IllegalArgumentException(
						"Blank path token detected! Invalid path -> " + Arrays.toString(pPath));
			}
			currentPath = currentPath.resolve(path);
		}

		return currentPath;
	}

	@Override
	protected DefaultResource registerNewResource(final URL pOriginContent, final boolean pReplaceExisting,
			final String[] pPath) throws IOException {
		final Path path = determinePath(pOriginContent, pPath);
		createDirectories(path.getParent());
		final Path absolutePath = path.toAbsolutePath();

		try (final InputStream in = pOriginContent.openStream()) {
			if (pReplaceExisting) {
				copy(in, absolutePath, REPLACE_EXISTING);
			} else {
				copy(in, absolutePath);
			}
		}

		workspace.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
		final DefaultResource file = new DefaultResource(getAsynListenerExecutor(), getTaskFactory(), pOriginContent,
				absolutePath, new CloseState());

		watcherThreadCache.put(absolutePath, file);
		return file;
	}

	/**
	 * @return
	 */
	private boolean syncClose() {
		boolean executeClose = false;
		synchronized (getManagedResourcesCache()) {
			if (!getState().isClosed()) {
				getState().close();
				executeClose = true;

				// Close all watcherThreadCache managed by this workspace
				// object.
				for (final Resource rs : getManagedResourcesCache().values()) {
					try {
						rs.close();
					} catch (final IOException e) {
						if (LOG.isDebugEnabled()) {
							LOG.debug(e.getMessage(), e);
						}
					}
				}

				// Clear the map which holds the origin-url to resource
				// mappings.
				getManagedResourcesCache().clear();

				// Set the interrupted flag on the watcher thread; this will
				// cause
				// an InterruptedException to be received by all waiting
				// threads.
				if (!watcherThread.isInterrupted()) {
					// Set the interrupted flag on the watcher-thread
					watcherThread.interrupt();
				}

				// Remove the shutdown hook
				try {
					runtime.removeShutdownHook(shutdownHook);
				} catch (final Exception e) {
					// Can happen if the vm is already shutting down i.e. close
					// has been called from the shutdown hook.
					if (LOG.isDebugEnabled()) {
						LOG.debug(e.getMessage(), e);
					}
				}
			}
		}
		return executeClose;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		if (syncClose()) {
			// Clear the concurrent-map which holds the mappings between
			// absolute paths to the resource objects.
			watcherThreadCache.clear();

			// Close the watchService retrieved from the workspace filesystem.
			try {
				watchService.close();
			} catch (final IOException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(e.getMessage(), e);
				}
			}

			// Inform the close-observer about the fact that this workspace has
			// been closed.
			closeObserver.closed(this);
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
			while (!getState().isClosed()) {
				final WatchKey key = watchService.take();

				for (final WatchEvent<?> event : key.pollEvents()) {
					final Kind<?> kind = event.kind();

					// Overflow event.
					if (OVERFLOW.equals(kind)) {
						continue; // loop
					}

					final Type typeOrNull = getTypeOrNull(kind);
					if (typeOrNull != null) {
						informListeners(workspace.resolve((Path) event.context()), typeOrNull);
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

	/**
	 * @param pPath
	 * @param pType
	 */
	private void informListeners(final Path pAbsolutePath, final ResourceEvent.Type pEventType) {
		final DefaultResource resource = watcherThreadCache.get(pAbsolutePath.toAbsolutePath());
		if (resource != null) {
			resource.informListeners(pEventType);
		} else if (LOG.isDebugEnabled()) {
			LOG.debug("No listener for " + pAbsolutePath);
		}
	}
}
