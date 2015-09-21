package ch.sourcepond.utils.fileobserver.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import javax.inject.Named;
import javax.inject.Singleton;

import ch.sourcepond.io.fileobserver.Workspace;
import ch.sourcepond.io.fileobserver.WorkspaceFactory;
import ch.sourcepond.utils.fileobserver.impl.dispatcher.EventDispatcher;
import ch.sourcepond.utils.fileobserver.impl.listener.ListenerRegistry;
import ch.sourcepond.utils.fileobserver.impl.replay.EventReplayFactory;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class DefaultWorkspaceFactory implements WorkspaceFactory {
	private final EventReplayFactory replayFactory;
	private final EventDispatcher dispatcher;
	private final ListenerRegistry registry;

	/**
	 * @param pReplayFactory
	 * @param pDispatcher
	 * @param pRegistry
	 */
	protected DefaultWorkspaceFactory(final EventReplayFactory pReplayFactory, final EventDispatcher pDispatcher,
			final ListenerRegistry pRegistry) {
		replayFactory = pReplayFactory;
		dispatcher = pDispatcher;
		registry = pRegistry;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.io.fileobserver.WorkspaceFactory#create(java.util.
	 * concurrent.Executor, java.nio.file.Path)
	 */
	@Override
	public Workspace create(final Executor pListenerNotifier, final Path pDirectory) throws IOException {
		return new DefaultWorkspace(new WorkspaceDirectory(pDirectory), replayFactory, dispatcher, registry);
	}
}
