package ch.sourcepond.utils.fileobserver.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.WorkspaceFactory;
import ch.sourcepond.utils.fileobserver.WorkspaceLockedException;

/**
 *
 */
public final class WatchManagerActivator implements BundleActivator, WorkspaceFactory, CloseObserver<DefaultWorkspace> {
	private final Set<DefaultWorkspace> workspaces = new HashSet<>();
	private final DefaultWorkspaceFactory factory = new DefaultWorkspaceFactory();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		context.registerService(WorkspaceFactory.class, this, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		synchronized (workspaces) {
			for (final DefaultWorkspace ws : workspaces) {
				ws.close();
			}
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
	public Workspace create(final Path pWorkspace, final ExecutorService pExecutor)
			throws WorkspaceLockedException, IOException {
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
