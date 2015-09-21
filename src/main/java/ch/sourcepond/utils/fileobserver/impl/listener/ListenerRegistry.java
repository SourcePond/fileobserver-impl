package ch.sourcepond.utils.fileobserver.impl.listener;

import java.util.Collection;

import ch.sourcepond.io.fileobserver.ResourceChangeListener;
import ch.sourcepond.io.fileobserver.ResourceFilter;

/**
 * @author rolandhauser
 *
 */
public interface ListenerRegistry {

	/**
	 * @param pFilter
	 * @param pListener
	 * @return
	 */
	boolean addListener(ResourceFilter pFilter, ResourceChangeListener pListener);

	/**
	 * @param pListener
	 * @return
	 */
	Collection<ResourceChangeListener> getListeners(ResourceFilter pListener);

	/**
	 * @param pListener
	 */
	void removeListener(ResourceChangeListener pListener);

	/**
	 * @return
	 */
	Collection<ResourceFilter> getFilters();
}
