package ch.sourcepond.utils.fileobserver.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import ch.sourcepond.utils.fileobserver.WorkspaceFactory;
import ch.sourcepond.utils.fileobserver.WorkspaceLockedException;

/**
 * @author rolandhauser
 *
 */
@Named // Necessary to let Eclipse Sisu discover this class
@Singleton
public class DefaultWorkspaceFactory implements WorkspaceFactory, CloseCallback<DefaultWorkspace> {
	private final Runtime runtime;
	private final TaskFactory taskFactory;

	/**
	 * @param pFs
	 */
	@Inject
	public DefaultWorkspaceFactory() {
		this(Runtime.getRuntime(), new TaskFactory());
	}

	/**
	 * @param pFs
	 */
	DefaultWorkspaceFactory(final Runtime pRuntime, final TaskFactory pTaskFactory) {
		runtime = pRuntime;
		taskFactory = pTaskFactory;
	}

	/**
	 * @param pStorageDirectory
	 * @return
	 * @throws IOException
	 */
	@Override
	public DefaultWorkspace create(final Path pWorkspace, final ExecutorService pObserverInformExecutor)
			throws WorkspaceLockedException, IOException {
		return create(pWorkspace, pObserverInformExecutor, this);
	}

	/**
	 * @param pWorkspace
	 * @param pObserverInformExecutor
	 * @param pCallback
	 * @return
	 * @throws WorkspaceLockedException
	 * @throws IOException
	 */
	DefaultWorkspace create(final Path pWorkspace, final ExecutorService pObserverInformExecutor,
			final CloseCallback<DefaultWorkspace> pCallback) throws WorkspaceLockedException, IOException {
		return new DefaultWorkspace(runtime, pWorkspace, taskFactory, pObserverInformExecutor, pCallback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.CloseCallback#closed(java.io.
	 * Closeable)
	 */
	@Override
	public void closed(final DefaultWorkspace pSource) {
		// noop
	}
}
