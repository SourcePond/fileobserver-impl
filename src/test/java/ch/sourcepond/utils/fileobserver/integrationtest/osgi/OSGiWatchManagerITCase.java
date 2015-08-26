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
package ch.sourcepond.utils.fileobserver.integrationtest.osgi;

import static ch.sourcepond.testing.bundle.OptionsHelper.defaultOptions;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import ch.sourcepond.utils.fileobserver.Constants;
import ch.sourcepond.utils.fileobserver.WorkspaceFactory;
import ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase;

/**
 * @author rolandhauser
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OSGiWatchManagerITCase extends WatchManagerITCase {

	@Inject
	private WorkspaceFactory manager;

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(final TestProbeBuilder probe) {
		probe.addTest(Constants.class);
		return probe;
	}

	@Configuration
	public Option[] config() throws Exception {
		return options(mavenBundle("ch.sourcepond.utils", "fileobserver-api").versionAsInProject(),
				mavenBundle("ch.sourcepond.utils", "fileobserver-commons").versionAsInProject(),
				mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(), defaultOptions());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase#
	 * getManager()
	 */
	@Override
	protected WorkspaceFactory verifyAndGetManager() {
		assertNotNull(manager);
		return manager;
	}
}
