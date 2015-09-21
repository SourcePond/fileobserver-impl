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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;

/**
 * @author rolandhauser
 *
 */
public class WorkspaceDirectory {
	private final Path directory;

	/**
	 * @param pDirectory
	 */
	WorkspaceDirectory(final Path pDirectory) {
		directory = pDirectory;
	}

	/**
	 * @param pOther
	 * @return
	 */
	public Path toAbsolutePath(final Path pOther) {
		return directory.resolve(pOther).toAbsolutePath();
	}

	/**
	 * @param pAbsolutePath
	 * @return
	 */
	public Path relativize(final Path pAbsolutePath) {
		return directory.relativize(pAbsolutePath);
	}

	/**
	 * @param pPath
	 * @return
	 */
	public Path resolve(final String[] pPath) {
		Path current = directory;
		for (final String sub : pPath) {
			current = current.resolve(sub);
		}
		return current;
	}

	/**
	 * @return
	 */
	public Path getPath() {
		return directory;
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public WatchService newWatchService() throws IOException {
		return directory.getFileSystem().newWatchService();
	}
}
