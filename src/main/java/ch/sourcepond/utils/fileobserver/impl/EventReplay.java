package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.io.fileobserver.ResourceEvent.Type.LISTENER_ADDED;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceFilter;

/**
 * @author rolandhauser
 *
 */
class EventReplay extends SimpleFileVisitor<Path> {
	private final WorkspaceDirectory directory;
	private final EventDispatcher dispatcher;
	private final ResourceFilter filter;
	private final ResourceChangeListener listener;

	/**
	 * @param pFilter
	 * @param pListener
	 */
	EventReplay(final WorkspaceDirectory pDirectory, final EventDispatcher pDispatcher,
			final ResourceFilter pFilter, final ResourceChangeListener pListener) {
		directory = pDirectory;
		dispatcher = pDispatcher;
		filter = pFilter;
		listener = pListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object,
	 * java.nio.file.attribute.BasicFileAttributes)
	 */
	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
		dispatcher.fireResourceChangeEvent(filter, listener, directory, dir, LISTENER_ADDED, true);
		return super.preVisitDirectory(dir, attrs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
	 * java.nio.file.attribute.BasicFileAttributes)
	 */
	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
		dispatcher.fireResourceChangeEvent(filter, listener, directory, file, LISTENER_ADDED, false);
		return super.visitFile(file, attrs);
	}

}
