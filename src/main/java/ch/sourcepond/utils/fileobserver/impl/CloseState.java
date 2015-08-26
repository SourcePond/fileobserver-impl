package ch.sourcepond.utils.fileobserver.impl;

/**
 * @author rolandhauser
 *
 */
class CloseState {
	private volatile boolean closed;

	/**
	 * 
	 */
	void checkClosed() {
		if (closed) {
			throw new IllegalStateException("Instance is closed!");
		}
	}

	/**
	 * @return
	 */
	boolean isClosed() {
		return closed;
	}

	/**
	 * 
	 */
	public void close() {
		closed = true;
	}
}
