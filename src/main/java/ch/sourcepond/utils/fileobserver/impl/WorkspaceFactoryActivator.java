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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.WorkspaceFactory;

/**
 *
 */
public final class WorkspaceFactoryActivator implements BundleActivator, WorkspaceFactory, CloseObserver<DefaultWorkspace> {
	private static final Logger LOG = getLogger(WorkspaceFactoryActivator.class);
	private final Set<DefaultWorkspace> workspaces;
	private final DefaultWorkspaceFactory factory;
	private ServiceRegistration<WorkspaceFactory> registration;

	/**
	 * 
	 */
	public WorkspaceFactoryActivator() {
		this(new DefaultWorkspaceFactory(), new HashSet<DefaultWorkspace>());
	}

	/**
	 * @param pFactory
	 */
	WorkspaceFactoryActivator(final DefaultWorkspaceFactory pFactory, final Set<DefaultWorkspace> pWorkspaces) {
		factory = pFactory;
		workspaces = pWorkspaces;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		registration = context.registerService(WorkspaceFactory.class, this, null);
		if (LOG.isInfoEnabled()) {
			LOG.info("Registered " + WorkspaceFactory.class.getName() + " service");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		if (registration != null) {
			registration.unregister();

			synchronized (workspaces) {
				for (final Iterator<DefaultWorkspace> it = workspaces.iterator(); it.hasNext();) {
					it.next().close();
					it.remove();
				}
			}
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Unregistered " + WorkspaceFactory.class.getName() + " service");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.WorkspaceFactory#watch(java.nio.file.
	 * Path, java.util.concurrent.ExecutorService)
	 */
	@Override
	public Workspace create(final Path pWorkspace, final ExecutorService pExecutor) throws IOException {
		final DefaultWorkspace workspace = factory.create(pWorkspace, pExecutor, this);
		synchronized (workspaces) {
			workspaces.add(workspace);
		}
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
		synchronized (workspaces) {
			workspaces.remove(pSource);
		}
	}
}
