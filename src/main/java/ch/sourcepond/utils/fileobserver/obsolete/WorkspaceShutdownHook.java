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
