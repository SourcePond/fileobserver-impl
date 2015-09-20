package ch.sourcepond.utils.fileobserver.impl;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;

import ch.sourcepond.io.fileobserver.Workspace;
import ch.sourcepond.io.fileobserver.WorkspaceFactory;

/**
 * @author rolandhauser
 *
 */
public class WorkspaceFactoryActivator implements BundleActivator, WorkspaceFactory {

	/**
	 * @author rolandhauser
	 *
	 */
	private class CloseInvocationHandler implements InvocationHandler {
		private final Workspace delegate;

		/**
		 * @param pDelegate
		 */
		CloseInvocationHandler(final Workspace pDelegate) {
			delegate = pDelegate;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
		 * java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			if ("close".equals(method.getName()) && args == null) {
				synchronized (workspaces) {
					workspaces.remove(proxy);
				}
			}
			return method.invoke(delegate, args);
		}
	}

	private static final Logger LOG = getLogger(WorkspaceFactoryActivator.class);
	private final Set<Workspace> workspaces = new HashSet<>();
	private final WorkspaceFactory workspaceFactory;

	/**
	 * @param pWorkspaceFactory
	 */
	WorkspaceFactoryActivator(final WorkspaceFactory pWorkspaceFactory) {
		workspaceFactory = pWorkspaceFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(final BundleContext context) {
		context.registerService(WorkspaceFactory.class, this, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) {
		synchronized (workspaces) {
			for (final Workspace workspace : workspaces) {
				try {
					workspace.close();
				} catch (final IOException e) {
					if (LOG.isWarnEnabled()) {
						LOG.warn(e.getMessage(), e);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.io.fileobserver.WorkspaceFactory#create(java.util.
	 * concurrent.Executor, java.nio.file.Path)
	 */
	@Override
	public Workspace create(final Executor pListenerNotifier, final Path pDirectory) throws IOException {
		final Workspace workspace = (Workspace) newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { Workspace.class },
				new CloseInvocationHandler(workspaceFactory.create(pListenerNotifier, pDirectory)));
		synchronized (workspaces) {
			workspaces.add(workspace);
		}
		return workspace;
	}

}
