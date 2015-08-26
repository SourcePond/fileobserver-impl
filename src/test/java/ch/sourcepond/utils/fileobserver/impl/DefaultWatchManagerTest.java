package ch.sourcepond.utils.fileobserver.impl;

import static org.mockito.Mockito.mock;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Before;

/**
 * @author rolandhauser
 *
 */
public class DefaultWatchManagerTest {
	private final Runtime runtime = mock(Runtime.class);
	private final ThreadFactory threadFactory = mock(ThreadFactory.class);
	private final TaskFactory taskFactory = mock(TaskFactory.class);
	private final FileSystem fs = mock(FileSystem.class);
	private final Path workspace = mock(Path.class);
	private final Path lockFile = mock(Path.class);
	private final DefaultWorkspaceFactory manager = new DefaultWorkspaceFactory(runtime, threadFactory, taskFactory);

	/**
	 * 
	 */
	@Before
	public void setup() {

	}
}
