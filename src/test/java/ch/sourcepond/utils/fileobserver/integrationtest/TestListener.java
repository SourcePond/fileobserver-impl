package ch.sourcepond.utils.fileobserver.integrationtest;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
public final class TestListener implements ResourceChangeListener {
	private final Lock lock = new ReentrantLock();
	private final Condition actionDoneCondition = lock.newCondition();
	private final Properties props = new Properties();
	private ResourceEvent event;
	private Exception exception;
	private boolean actionDone;

	/**
	 * @return
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * @return
	 */
	public Properties getProperties() {
		return props;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public ResourceEvent getEvent() {
		return event;
	}

	/**
	 * @param pTest
	 * @throws Exception
	 */
	public void awaitAction(final TestAction pTest) throws Exception {
		lock.lock();
		try {
			int c = 0;
			while (!actionDone) {
				actionDoneCondition.await(500, MILLISECONDS);

				if (c++ > 100) {
					fail("Action not executed!");
				}
			}
			pTest.test();
		} finally {
			actionDone = false;
			lock.unlock();
		}
	}

	/**
	 * @param pEvent
	 */
	@Override
	public void resourceChange(final ResourceEvent pEvent) {
		lock.lock();
		try (final InputStream in = pEvent.getSource().open()) {
			props.load(in);
		} catch (final IOException e) {
			exception = e;
		} finally {
			actionDone = true;
			event = pEvent;
			actionDoneCondition.signalAll();
			lock.unlock();
		}
	}
}
