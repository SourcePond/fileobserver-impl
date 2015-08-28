package ch.sourcepond.utils.fileobserver.impl;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.commons.CloseState;

/**
 * @author rolandhauser
 *
 */
public class DefaultWorkspaceTest extends BaseDefaultWorkspaceTest {
	private final Thread shutdownHook = mock(Thread.class);
	private final OutputStream fileOut = mock(OutputStream.class);
	private final InputStream originContentStream = mock(InputStream.class);
	private final URLConnection originContentConn = mock(URLConnection.class);
	private final URLStreamHandler originContentHandler = new URLStreamHandler() {

		@Override
		protected URLConnection openConnection(final URL u) throws IOException {
			return originContentConn;
		}
	};
	private URL originContent;

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

		workspace.setShutdownHook(shutdownHook);

		when(originContentConn.getInputStream()).thenReturn(originContentStream);
		originContent = new URL("http", "example.com", 29395, "any/content.properties", originContentHandler);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.impl.BaseDefaultWorkspaceTest#newState()
	 */
	@Override
	protected CloseState newState() {
		return new CloseState();
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
		order.verify(provider).newInputStream(absoluteFile);

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
		order.verify(provider).newInputStream(absoluteFile);

		// Once created a resource must be shared
		assertSame(res, workspace.watchFile(originContent, false, DIR_1, DIR_2, FILE));
		order.verifyNoMoreInteractions();
	}

	/**
	 * 
	 */
	@Test
	public void verifyCloseClearCaches() throws IOException {
		when(provider.newOutputStream(file, CREATE_NEW, WRITE)).thenReturn(fileOut);
		workspace.watchFile(originContent, DIR_1, DIR_2, FILE);
		assertEquals(1, managedResourcesCache.size());
		assertEquals(1, watcherThreadCache.size());

		// This should remove the resource from the internal caches
		workspace.close();
		assertTrue(managedResourcesCache.isEmpty());
		assertTrue(watcherThreadCache.isEmpty());
	}

	/**
	 * 
	 */
	@Test
	public void verifyCloseInterruptWatcherThread() {
		workspace.close();

		// Should have no effect
		workspace.close();
		verify(watcherThread).interrupt();
	}

	/**
	 * 
	 */
	@Test
	public void verifyWatcherThreadAlreadyInterrupted() {
		when(watcherThread.isInterrupted()).thenReturn(true);
		workspace.close();

		// Should have no effect
		workspace.close();
		verify(watcherThread, never()).interrupt();
	}

	/**
	 * 
	 */
	@Test
	public void verifyRemoveShutdownHook() {
		workspace.close();

		// Should have no effect
		workspace.close();
		verify(runtime).removeShutdownHook(shutdownHook);
	}

	/**
	 * 
	 */
	@Test
	public void verifyNoExceptionWhenVmShuttingDown() {
		final IllegalStateException expected = new IllegalStateException();
		doThrow(expected).when(runtime).removeShutdownHook(shutdownHook);

		// This should not cause an exception to be thrown
		workspace.close();
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyCloseWatchService() throws IOException {
		workspace.close();

		// Should have no effect
		workspace.close();

		verify(watchService).close();
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyCloseWatchServiceHasThrownException() throws IOException {
		doThrow(IOException.class).when(watchService).close();

		// Should not cause an exception to be thrown.
		workspace.close();

		verify(watchService).close();
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyCloseInformObserver() throws IOException {
		workspace.close();

		// Should have no effect
		workspace.close();

		verify(closeObserver).closed(workspace);
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void verifyCloseResources() throws IOException {
		when(provider.newOutputStream(file, CREATE_NEW, WRITE)).thenReturn(fileOut);
		final Resource res = workspace.watchFile(originContent, DIR_1, DIR_2, FILE);
		workspace.close();

		// Should have no effect
		workspace.close();

		try {
			res.openStream();
			fail("Exception expected here");
		} catch (final IOException expected) {
			// expected
		}
	}
}
