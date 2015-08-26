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
package ch.sourcepond.utils.fileobserver.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Workspace;
import ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactoryActivator;

/**
 *
 */
public final class WorkspaceFactoryActivator
		extends BaseWorkspaceFactoryActivator<DefaultResource, DefaultWorkspace, DefaultWorkspaceFactory> {
	private static final Logger LOG = getLogger(WorkspaceFactoryActivator.class);

	/**
	 * 
	 */
	public WorkspaceFactoryActivator() {
		this(new DefaultWorkspaceFactory(), new HashSet<Workspace>());
	}

	/**
	 * @param pFactory
	 */
	WorkspaceFactoryActivator(final DefaultWorkspaceFactory pFactory, final Set<Workspace> pWorkspaces) {
		super(pFactory, pWorkspaces);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.commons.BaseWorkspaceFactoryActivator#
	 * getLog()
	 */
	@Override
	protected Logger getLog() {
		return LOG;
	}
}
