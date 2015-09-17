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
package ch.sourcepond.utils.fileobserver.integrationtest.jsr330;

import static com.google.inject.Guice.createInjector;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.After;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

import ch.sourcepond.utils.fileobserver.WorkspaceFactory;
import ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase;
import ch.sourcepond.utils.fileobserver.obsolete.DefaultWorkspaceFactory;

/**
 * @author rolandhauser
 *
 */
public class CdiWatchManagerITCase extends WatchManagerITCase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase#
	 * verifyAndGetManager()
	 */
	@Override
	protected WorkspaceFactory verifyAndGetManager() {
		final Injector injector = createInjector(new Module() {

			@Override
			public void configure(final Binder binder) {
				binder.bind(WorkspaceFactory.class).to(DefaultWorkspaceFactory.class);
			}
		});
		final WorkspaceFactory watchManager = injector.getInstance(WorkspaceFactory.class);
		// Verify that the watch-manager is a singleton
		assertSame(watchManager, injector.getInstance(WorkspaceFactory.class));
		return watchManager;
	}

	/**
	 * @throws IOException
	 * 
	 */
	@After
	public void tearDown() throws IOException {
		if (getWatcher() != null) {
			getWatcher().close();
		}
	}
}
