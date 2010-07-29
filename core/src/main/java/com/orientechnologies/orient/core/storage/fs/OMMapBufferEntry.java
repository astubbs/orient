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
package com.orientechnologies.orient.core.storage.fs;

import java.nio.MappedByteBuffer;

public class OMMapBufferEntry {
	OFileMMap					file;
	MappedByteBuffer	buffer;
	int								beginOffset;
	int								size;
	int								counter;
	boolean						pin;

	public OMMapBufferEntry(final OFileMMap iFile, final MappedByteBuffer buffer, final int beginOffset, final int size) {
		this.file = iFile;
		this.buffer = buffer;
		this.beginOffset = beginOffset;
		this.size = size;
		this.counter = 0;
		pin = false;
	}
}