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

import static ch.sourcepond.io.fileobserver.ResourceEvent.Type.LISTENER_ADDED;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceFilter;
import ch.sourcepond.utils.fileobserver.impl.WorkspaceDirectory;
import ch.sourcepond.utils.fileobserver.impl.dispatcher.EventDispatcher;

/**
 * @author rolandhauser
 *
 */
final class EventReplay extends SimpleFileVisitor<Path> {
	private final WorkspaceDirectory directory;
	private final EventDispatcher dispatcher;
	private final ResourceFilter filter;
	private final ResourceChangeListener listener;

	/**
	 * @param pFilter
	 * @param pListener
	 */
	EventReplay(final WorkspaceDirectory pDirectory, final EventDispatcher pDispatcher, final ResourceFilter pFilter,
			final ResourceChangeListener pListener) {
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
		dispatcher.fireResourceChangeEvent(filter, listener, directory, dir, LISTENER_ADDED);
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
		dispatcher.fireResourceChangeEvent(filter, listener, directory, file, LISTENER_ADDED);
		return super.visitFile(file, attrs);
	}

}
