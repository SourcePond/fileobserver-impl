package ch.sourcepond.utils.fileobserver.impl;

import java.nio.file.Path;

import ch.sourcepond.utils.fileobserver.Workspace;

/**
 *
 */
class ThreadFactory {

	/**
	 * @param pWorkspace
	 * @param pWorkspacePath
	 * @return
	 */
	Thread newWatcher(final Runnable pWorkspace, final Path pWorkspacePath) {
		return new Thread(pWorkspace, getClass().getSimpleName() + ": " + pWorkspacePath.toAbsolutePath());
	}

	/**
	 * @param pWorkspace
	 * @return
	 */
	Thread newShutdownHook(final Workspace pWorkspace) {
		return new WorkspaceShutdownHook(pWorkspace);
	}
}
