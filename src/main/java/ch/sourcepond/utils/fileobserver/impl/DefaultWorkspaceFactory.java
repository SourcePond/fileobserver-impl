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

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import ch.sourcepond.utils.fileobserver.WorkspaceFactory;

/**
 * @author rolandhauser
 *
 */
@Named // Necessary to let Eclipse Sisu discover this class
@Singleton
public class DefaultWorkspaceFactory implements WorkspaceFactory, CloseObserver<DefaultWorkspace> {
	private final Runtime runtime;
	private final ThreadFactory threadFactory;
	private final TaskFactory taskFactory;

	/**
	 * @param pFs
	 */
	@Inject
	public DefaultWorkspaceFactory() {
		this(Runtime.getRuntime(), new ThreadFactory(), new TaskFactory());
	}

	/**
	 * @param pFs
	 */
	DefaultWorkspaceFactory(final Runtime pRuntime, final ThreadFactory pThreadFactory,
			final TaskFactory pTaskFactory) {
		runtime = pRuntime;
		threadFactory = pThreadFactory;
		taskFactory = pTaskFactory;
	}

	/**
	 * @param pStorageDirectory
	 * @return
	 * @throws IOException
	 */
	@Override
	public DefaultWorkspace create(final Path pWorkspace, final ExecutorService pObserverInformExecutor)
			throws IOException {
		return create(pWorkspace, pObserverInformExecutor, this);
	}

	/**
	 * @param pWorkspace
	 * @param pObserverInformExecutor
	 * @param pCallback
	 * @return
	 * @throws WorkspaceLockedException
	 * @throws IOException
	 */
	DefaultWorkspace create(final Path pWorkspace, final ExecutorService pObserverInformExecutor,
			final CloseObserver<DefaultWorkspace> pCallback) throws IOException {
		// Create workspace instance
		final DefaultWorkspace workspace = new DefaultWorkspace(runtime, pWorkspace, taskFactory,
				pObserverInformExecutor, pCallback);

		// Create and set all necessary threads on workspace
		final Thread watcherThread = threadFactory.newWatcher(workspace, pWorkspace);
		final Thread shutdownHook = threadFactory.newShutdownHook(workspace);
		workspace.setWatcherThread(watcherThread);
		workspace.setShutdownHook(shutdownHook);

		// Start the watcher thread
		watcherThread.start();

		return workspace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.CloseObserver#closed(java.io.
	 * Closeable)
	 */
	@Override
	public void closed(final DefaultWorkspace pSource) {
		// noop
	}
}
