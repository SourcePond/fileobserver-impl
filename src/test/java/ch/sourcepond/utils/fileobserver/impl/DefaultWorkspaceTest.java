package ch.sourcepond.utils.fileobserver.impl;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * @author rolandhauser
 *
 */
public class DefaultWorkspaceTest {
	private static final String DIR_1 = "dir1";
	private static final String DIR_2 = "dir2";
	private static final String FILE = "file";
	private final Thread shutdownHook = mock(Thread.class);
	private final Thread watcherThread = mock(Thread.class);
	private final Runtime runtime = mock(Runtime.class);
	private final FileSystem fs = mock(FileSystem.class);
	private final FileSystemProvider provider = mock(FileSystemProvider.class);
	private final WatchService watchService = mock(WatchService.class);
	private final Path workspacePath = mock(Path.class);
	private final Path dir1 = mock(Path.class);
	private final Path dir2 = mock(Path.class);
	private final Path file = mock(Path.class);
	private final Path absoluteFile = mock(Path.class);
	private final OutputStream fileOut = mock(OutputStream.class);
	private final TaskFactory taskFactory = mock(TaskFactory.class);
	private final ExecutorService executor = mock(ExecutorService.class);
	@SuppressWarnings("unchecked")
	private final CloseObserver<DefaultWorkspace> closeObserver = mock(CloseObserver.class);
	private final InputStream originContentStream = mock(InputStream.class);
	private final URLConnection originContentConn = mock(URLConnection.class);
	private final URLStreamHandler originContentHandler = new URLStreamHandler() {

		@Override
		protected URLConnection openConnection(final URL u) throws IOException {
			return originContentConn;
		}
	};
	private URL originContent;
	private DefaultWorkspace workspace;

	/**
	 * @throws IOException
	 * 
	 */
	@Before
	public void setup() throws IOException {
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

		workspace = new DefaultWorkspace(runtime, workspacePath, taskFactory, executor, closeObserver);
		workspace.setWatcherThread(watcherThread);
		workspace.setShutdownHook(shutdownHook);

		when(originContentConn.getInputStream()).thenReturn(originContentStream);
		originContent = new URL("http", "example.com", 29395, "any/content.properties", originContentHandler);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = NullPointerException.class)
	public void tryWatchFileWithNullOrigin() throws IOException {
		workspace.watchFile(null, DIR_1, DIR_2, FILE);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void tryWatchFileWithNoPathTokens() throws IOException {
		workspace.watchFile(originContent);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void tryWatchFileWithNullPathToken() throws IOException {
		workspace.watchFile(originContent, DIR_1, DIR_2, null);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void tryWatchFileWithEmptyPathToken() throws IOException {
		workspace.watchFile(originContent, DIR_1, "", FILE);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void tryWatchFileWithBlankPathToken() throws IOException {
		workspace.watchFile(originContent, "	", DIR_2, FILE);
	}

	/**
	 * @throws IOException
	 */
	@Test(expected = IllegalStateException.class)
	public void tryWatchWhenClosed() throws IOException {
		workspace.close();
		workspace.watchFile(originContent, DIR_1, DIR_2, FILE);
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void watchFileReplaceExisting() throws IOException {
		when(provider.newOutputStream(file, CREATE_NEW, WRITE)).thenReturn(fileOut);
		final DefaultResource res = (DefaultResource) workspace.watchFile(originContent, DIR_1, DIR_2, FILE);

		assertSame(absoluteFile, res.getStoragePath());
		assertSame(originContent, res.getOriginContent());

		final InOrder order = inOrder(provider, workspacePath);
		order.verify(provider).createDirectory(dir2);
		order.verify(provider).deleteIfExists(absoluteFile);
		order.verify(provider).newOutputStream(absoluteFile, CREATE_NEW, WRITE);
		order.verify(workspacePath).register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

		// Once created a resource must be shared
		assertSame(res, workspace.watchFile(originContent, DIR_1, DIR_2, FILE));
		order.verifyNoMoreInteractions();
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void watchFileDoNotReplace() throws IOException {
		when(provider.newOutputStream(file, CREATE_NEW, WRITE)).thenReturn(fileOut);
		final DefaultResource res = (DefaultResource) workspace.watchFile(originContent, false, DIR_1, DIR_2, FILE);

		assertSame(absoluteFile, res.getStoragePath());
		assertSame(originContent, res.getOriginContent());

		final InOrder order = inOrder(provider, workspacePath);
		order.verify(provider, never()).deleteIfExists(absoluteFile);
		order.verify(provider).newOutputStream(absoluteFile, CREATE_NEW, WRITE);
		order.verify(workspacePath).register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

		// Once created a resource must be shared
		assertSame(res, workspace.watchFile(originContent, false, DIR_1, DIR_2, FILE));
		order.verifyNoMoreInteractions();
	}
}
