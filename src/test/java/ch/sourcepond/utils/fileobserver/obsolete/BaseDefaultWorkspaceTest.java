package ch.sourcepond.utils.fileobserver.obsolete;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.junit.Before;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.commons.CloseObserver;
import ch.sourcepond.utils.fileobserver.commons.CloseState;
import ch.sourcepond.utils.fileobserver.commons.TaskFactory;
import ch.sourcepond.utils.fileobserver.obsolete.DefaultResource;
import ch.sourcepond.utils.fileobserver.obsolete.DefaultWorkspace;

/**
 *
 */
public abstract class BaseDefaultWorkspaceTest {
	static final String DIR_1 = "dir1";
	static final String DIR_2 = "dir2";
	static final String FILE = "file";
	protected final Thread watcherThread = mock(Thread.class);
	protected final Runtime runtime = mock(Runtime.class);
	protected final TaskFactory taskFactory = mock(TaskFactory.class);
	protected final ExecutorService executor = mock(ExecutorService.class);
	@SuppressWarnings("unchecked")
	protected final CloseObserver closeObserver = mock(CloseObserver.class);
	protected final CloseState state = newState();
	protected final FileSystem fs = mock(FileSystem.class);
	protected final FileSystemProvider provider = mock(FileSystemProvider.class);
	protected final Path dir1 = mock(Path.class);
	protected final Path dir2 = mock(Path.class);
	protected final Path file = mock(Path.class);
	protected final Path absoluteFile = mock(Path.class);
	protected final InputStream storagePathInputStream = new ByteArrayInputStream(new byte[0]);
	protected final WatchService watchService = mock(WatchService.class);
	protected final Path workspacePath = mock(Path.class);
	protected final Map<URL, Resource> managedResourcesCache = new HashMap<>();
	protected final ConcurrentMap<Path, DefaultResource> watcherThreadCache = new ConcurrentHashMap<>();
	protected DefaultWorkspace workspace;

	/**
	 * @throws IOException
	 */
	@Before
	public void baseSetup() throws IOException {
		when(workspacePath.getFileSystem()).thenReturn(fs);
		when(fs.newWatchService()).thenReturn(watchService);
		when(fs.provider()).thenReturn(provider);
		when(workspacePath.resolve(DIR_1)).thenReturn(dir1);
		when(dir1.getParent()).thenReturn(workspacePath);
		when(dir1.getFileSystem()).thenReturn(fs);
		when(dir1.resolve(DIR_2)).thenReturn(dir2);
		when(dir2.getParent()).thenReturn(dir1);
		when(dir2.getFileSystem()).thenReturn(fs);
		when(dir2.resolve(FILE)).thenReturn(file);
		when(file.getParent()).thenReturn(dir2);
		when(file.getFileSystem()).thenReturn(fs);
		when(file.toAbsolutePath()).thenReturn(absoluteFile);
		when(absoluteFile.getFileSystem()).thenReturn(fs);
		when(provider.newInputStream(absoluteFile)).thenReturn(storagePathInputStream);

		workspace = new DefaultWorkspace(runtime, state, workspacePath, taskFactory, executor, closeObserver,
				managedResourcesCache, watcherThreadCache);
		workspace.setWatcherThread(watcherThread);
	}

	/**
	 * @return
	 */
	protected abstract CloseState newState();
}