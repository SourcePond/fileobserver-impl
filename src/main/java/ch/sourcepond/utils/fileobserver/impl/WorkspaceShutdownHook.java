package ch.sourcepond.utils.fileobserver.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Workspace;

/**
 * @author rolandhauser
 *
 */
final class WorkspaceShutdownHook extends Thread {
	private static final Logger LOG = getLogger(WorkspaceShutdownHook.class);
	private final Workspace workspace;

	/**
	 * @param pWorkspace
	 */
	WorkspaceShutdownHook(final Workspace pWorkspace) {
		workspace = pWorkspace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			workspace.close();
		} catch (final IOException e) {
			LOG.warn(e.getMessage(), e);
		}
	}
}
