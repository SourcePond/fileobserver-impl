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
package ch.sourcepond.utils.fileobserver.obsolete;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactory;
import ch.sourcepond.utils.fileobserver.commons.CloseState;
import ch.sourcepond.utils.fileobserver.commons.TaskFactory;

/**
 *
 */
@Named // Necessary to let Eclipse Sisu discover this class
@Singleton
public class DefaultWorkspaceFactory extends BaseWorkspaceFactory {
	private static final Logger LOG = getLogger(DefaultWorkspaceFactory.class);
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
	 * ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactory#doCreate(
	 * java.util.concurrent.ExecutorService,
	 * java.util.concurrent.ExecutorService, java.nio.file.Path)
	 */
	@Override
	protected Workspace doCreate(final ExecutorService pListenerNotifier, final ExecutorService pChecksumCalculator,
			final Path pWorkspace) throws IOException {
		// Create workspace instance
		final DefaultWorkspace workspace = new DefaultWorkspace(runtime, new CloseState(), pWorkspace, taskFactory,
				pListenerNotifier, this, new HashMap<URL, Resource>(), new ConcurrentHashMap<Path, DefaultResource>());

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
	 * @see
	 * ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactory#getLog()
	 */
	@Override
	protected Logger getLog() {
		return LOG;
	}
}
