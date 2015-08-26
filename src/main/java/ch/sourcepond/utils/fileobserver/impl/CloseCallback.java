package ch.sourcepond.utils.fileobserver.impl;

import java.io.Closeable;

/**
 * @author rolandhauser
 *
 */
interface CloseCallback<T extends Closeable> {

	/**
	 * 
	 */
	void closed(T pSource);
}
