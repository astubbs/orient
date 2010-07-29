/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.storage.impl.local;

import java.io.IOException;

import com.orientechnologies.common.concur.resource.OSharedResourceAdaptiveLinked;
import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.config.OStorageFileConfiguration;
import com.orientechnologies.orient.core.storage.fs.OFile;
import com.orientechnologies.orient.core.storage.fs.OFileFactory;

public class OSingleFileSegment extends OSharedResourceAdaptiveLinked {
	protected OStorageLocal							storage;
	protected OFile											file;
	protected OStorageFileConfiguration	config;

	public OSingleFileSegment(OStorageLocal iStorage, final OStorageFileConfiguration iConfig) throws IOException {
		super(iStorage);
		config = iConfig;
		storage = iStorage;
		file = OFileFactory.create(iConfig.type, iStorage.getVariableParser().resolveVariables(iConfig.path), iStorage.getMode());
		file.setMaxSize((int) OFileUtils.getSizeAsNumber(iConfig.maxSize));
		file.setIncrementSize((int) OFileUtils.getSizeAsNumber(iConfig.incrementSize));
	}

	public boolean open() throws IOException {
		try {
			acquireExclusiveLock();

			boolean softClosed = file.open();
			if (!softClosed) {
				// LAST TIME THE FILE WAS NOT CLOSED IN SOFT WAY
				OLogManager.instance().warn(this,
						"File " + file.getOsFile().getAbsolutePath() + " was not closed correctly last time. Checking segments...");
			}

			return softClosed;
		} finally {
			releaseExclusiveLock();
		}
	}

	public void create(int iStartSize) throws IOException {
		try {
			acquireExclusiveLock();

			file.create(iStartSize);
		} finally {
			releaseExclusiveLock();
		}
	}

	public void close() throws IOException {
		try {
			acquireExclusiveLock();

			if (file != null)
				file.close();

		} finally {
			releaseExclusiveLock();
		}
	}

	public void delete() throws IOException {
		try {
			acquireExclusiveLock();

			if (file != null)
				file.delete();

		} finally {
			releaseExclusiveLock();
		}
	}

	public long getSize() {
		return file.getFileSize();
	}

	public long getFilledUpTo() {
		return file.getFilledUpTo();
	}

	public OStorageFileConfiguration getConfig() {
		return config;
	}
}
