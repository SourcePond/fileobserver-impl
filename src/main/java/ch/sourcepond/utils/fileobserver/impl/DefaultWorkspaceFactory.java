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
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactory;
import ch.sourcepond.utils.fileobserver.commons.CloseObserver;
import ch.sourcepond.utils.fileobserver.commons.CloseState;
import ch.sourcepond.utils.fileobserver.commons.TaskFactory;

/**
 *
 */
@Named // Necessary to let Eclipse Sisu discover this class
@Singleton
public class DefaultWorkspaceFactory extends BaseWorkspaceFactory<DefaultResource, DefaultWorkspace> {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactory#create(ch.
	 * sourcepond.utils.fileobserver.commons.CloseObserver,
	 * java.util.concurrent.ExecutorService, java.nio.file.FileSystem,
	 * java.lang.String)
	 */
	@Override
	protected DefaultWorkspace create(final CloseObserver<DefaultWorkspace> pCloseObserver,
			final ExecutorService pAsynListenerExecutor, final FileSystem pFs, final String pBaseWorkspacePath,
			final String... pWorkspacePath) throws IOException {
		final Path workspacePath = pFs.getPath(pBaseWorkspacePath, pWorkspacePath);

		// Create workspace instance
		final DefaultWorkspace workspace = new DefaultWorkspace(runtime, new CloseState(), workspacePath, taskFactory,
				pAsynListenerExecutor, pCloseObserver, new HashMap<URL, DefaultResource>(),
				new ConcurrentHashMap<Path, DefaultResource>());

		// Create and set all necessary threads on workspace
		final Thread watcherThread = threadFactory.newWatcher(workspace, workspacePath);
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
