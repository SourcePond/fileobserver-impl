package ch.sourcepond.utils.fileobserver.impl;

import java.nio.file.Path;

/**
 * @author rolandhauser
 *
 */
class WorkspaceDirectory {
	private final Path directory;

	WorkspaceDirectory(final Path pDirectory) {
		directory = pDirectory;
	}

	Path toAbsolutePath(final Path pOther) {
		return directory.resolve(pOther).toAbsolutePath();
	}

	Path relativize(final Path pAbsolutePath) {
		return directory.relativize(pAbsolutePath);
	}

	/**
	 * @param pPath
	 * @return
	 */
	Path resolve(final String[] pPath) {
		Path current = directory;
		for (final String sub : pPath) {
			current = current.resolve(sub);
		}
		return current;
	}

	Path getPath() {
		return directory;
	}

}
