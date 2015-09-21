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
package ch.sourcepond.utils.fileobserver.impl.replay;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceFilter;
import ch.sourcepond.utils.fileobserver.impl.WorkspaceDirectory;
import ch.sourcepond.utils.fileobserver.impl.dispatcher.EventDispatcher;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class DefaultEventReplayFactory {
	private final EventDispatcher dispatcher;

	/**
	 * @param pDispatcher
	 */
	@Inject
	DefaultEventReplayFactory(final EventDispatcher pDispatcher) {
		dispatcher = pDispatcher;
	}

	/**
	 * @param pDirectory
	 * @param pFilter
	 * @param pListener
	 * @return
	 */
	public EventReplay newReplay(final WorkspaceDirectory pDirectory, final ResourceFilter pFilter,
			final ResourceChangeListener pListener) {
		return new EventReplay(pDirectory, dispatcher, pFilter, pListener);
	}
}
