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
