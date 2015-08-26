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

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.Workspace;

/**
 * @author rolandhauser
 *
 */
class TaskFactory {

	/**
	 * @param pWorkspace
	 * @return
	 */
	Thread newShutdownHook(final Workspace pWorkspace) {
		return new WorkspaceShutdownHook(pWorkspace);
	}

	/**
	 * @param pObserver
	 * @param pResource
	 * @return
	 */
	Runnable newObserverTask(final ResourceChangeListener pObserver, final ResourceEvent pEvent) {
		return new InformObserverTask(pObserver, pEvent);
	}
}
