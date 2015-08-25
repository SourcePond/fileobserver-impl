package ch.sourcepond.utils.fileobserver.integrationtest.osgi;

import static ch.sourcepond.testing.bundle.OptionsHelper.defaultOptions;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import ch.sourcepond.utils.fileobserver.WatchManager;
import ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase;

/**
 * @author rolandhauser
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OSGiWatchManagerITCase extends WatchManagerITCase {

	@Inject
	private WatchManager manager;

	@Configuration
	public Option[] config() throws Exception {
		return options(mavenBundle("ch.sourcepond.utils", "fileobserver-api").versionAsInProject(),
				mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(), defaultOptions());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.integrationtest.WatchManagerITCase#
	 * getManager()
	 */
	@Override
	protected WatchManager verifyAndGetManager() {
		assertNotNull(manager);
		return manager;
	}
}
