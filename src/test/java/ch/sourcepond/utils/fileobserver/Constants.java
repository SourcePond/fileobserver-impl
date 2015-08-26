package ch.sourcepond.utils.fileobserver;

import static java.nio.file.FileSystems.getDefault;

import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;

/**
 *
 */
public interface Constants {
	/**
	 * Concrete file name of the test file located in src/test/resources
	 */
	String TEST_FILE_NAME = "test.properties";

	/**
	 * Default test workspace -> target
	 */
	Path WORKSPACE = getDefault().getPath(SystemUtils.USER_DIR, "target");
}
