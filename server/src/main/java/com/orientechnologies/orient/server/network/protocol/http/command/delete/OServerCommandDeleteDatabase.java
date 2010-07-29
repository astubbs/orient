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
package com.orientechnologies.orient.server.network.protocol.http.command.delete;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.db.OSharedDocumentDatabase;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedDbAbstract;

public class OServerCommandDeleteDatabase extends OServerCommandAuthenticatedDbAbstract {
	private static final String[]	NAMES	= { "DELETE.database" };

	public void execute(final OHttpRequest iRequest) throws Exception {
		String[] urlParts = checkSyntax(iRequest.url, 2, "Syntax error: database/<database>");

		iRequest.data.commandInfo = "Delete database";
		iRequest.data.commandDetail = urlParts[1];

		ODatabaseDocumentTx db = null;

		try {
			if (!OSharedDocumentDatabase.getDatabasePools().containsKey(urlParts[1]))
				throw new IllegalArgumentException("Invalid database '" + urlParts[1] + "'");

			throw new UnsupportedOperationException("Delete database");

		} finally {
			if (db != null)
				db.close();
		}

		// sendTextContent(iRequest, OHttpUtils.STATUS_OK_CODE, OHttpUtils.STATUS_OK_DESCRIPTION, null, OHttpUtils.CONTENT_TEXT_PLAIN,
		// null);
	}

	public String[] getNames() {
		return NAMES;
	}
}
