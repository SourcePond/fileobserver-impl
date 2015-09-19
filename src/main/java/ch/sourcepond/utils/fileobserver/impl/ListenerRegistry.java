package ch.sourcepond.utils.fileobserver.impl;

import static com.google.common.collect.HashMultimap.create;

import java.util.Collection;

import com.google.common.collect.SetMultimap;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceFilter;

/**
 * @author rolandhauser
 *
 */
class ListenerRegistry {
	private final SetMultimap<ResourceFilter, ResourceChangeListener> listeners = create();

	/**
	 * @param pFilter
	 * @param pListener
	 * @return
	 */
	boolean addListener(final ResourceFilter pFilter, final ResourceChangeListener pListener) {
		if (listeners.put(pFilter, pListener)) {

		}

		return false;
	}

	Collection<ResourceChangeListener> getListeners(final ResourceFilter pListener) {

		return null;
	}

	void removeListener(final ResourceChangeListener pListener) {
		// TODO Auto-generated method stub

	}
}
