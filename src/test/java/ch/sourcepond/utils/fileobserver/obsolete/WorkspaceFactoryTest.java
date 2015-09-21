package ch.sourcepond.utils.fileobserver.obsolete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.WorkspaceFactory;
import ch.sourcepond.utils.fileobserver.obsolete.DefaultWorkspace;
import ch.sourcepond.utils.fileobserver.obsolete.DefaultWorkspaceFactory;
import ch.sourcepond.utils.fileobserver.obsolete.WorkspaceFactoryActivator;

/**
 *
 */
@Ignore
public class WorkspaceFactoryTest {
	private static final String WORKSPACE_PATH = "anyPath";
	private final BundleContext context = mock(BundleContext.class);
	private final FileSystem fs = mock(FileSystem.class);
	private final ExecutorService executor = mock(ExecutorService.class);
	private final DefaultWorkspaceFactory factory = mock(DefaultWorkspaceFactory.class);
	private final DefaultWorkspace workspace = mock(DefaultWorkspace.class);
	private final ServiceRegistration<WorkspaceFactory> registration = mock(ServiceRegistration.class);
	private final Set<Workspace> openWorkspaces = new HashSet<>();
	private final WorkspaceFactoryActivator activator = new WorkspaceFactoryActivator(factory);

	/**
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		// when(builder.create(activator, executor, fs,
		// WORKSPACE_PATH)).thenReturn(workspace);
		when(context.registerService(WorkspaceFactory.class, activator, null)).thenReturn(registration);
	}

	/**
	 * 
	 */
	@Test
	public void verifyDefaultConstructor() {
		// This should not cause an exception to be thrown.
		// new WorkspaceFactoryActivator();
	}

	/**
	 * 
	 */
	@Test
	public void verifyStartCreateStop() throws Exception {
		activator.start(context);
		assertSame(workspace, activator.create(executor, fs, WORKSPACE_PATH));
		assertEquals(1, openWorkspaces.size());
		activator.stop(context);
		assertTrue(openWorkspaces.isEmpty());

		final InOrder order = inOrder(registration, workspace);
		order.verify(registration).unregister();
		order.verify(workspace).close();
	}

	/**
	 * @throws IOException
	 * 
	 */
	@Test
	public void verifyCloseObserver() throws IOException {
		activator.create(executor, fs, WORKSPACE_PATH);
		// activator.closed(workspace);
		assertTrue(openWorkspaces.isEmpty());
	}
}
