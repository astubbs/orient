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
package com.orientechnologies.orient.server.network.protocol.http.command.post;

import java.util.List;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.server.db.OSharedDocumentDatabase;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpUtils;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedDbAbstract;

public class OServerCommandPostCommand extends OServerCommandAuthenticatedDbAbstract {
	private static final String[]	NAMES	= { "POST|command/*" };

	@SuppressWarnings("unchecked")
	public void execute(final OHttpRequest iRequest) throws Exception {
		String[] urlParts = checkSyntax(iRequest.url, 4, "Syntax error: command/sql/<command-text>");

		final String text = urlParts[3].trim();

		iRequest.data.commandInfo = "Command";
		iRequest.data.commandDetail = text;

		ODatabaseDocumentTx db = null;

		final Object response;

		try {
			db = getProfiledDatabaseInstance(iRequest, urlParts[2]);

			response = db.command(new OCommandSQL(text)).execute();

		} finally {
			if (db != null)
				OSharedDocumentDatabase.release(db);
		}

		if (response instanceof List<?>)
			sendRecordsContent(iRequest, (List<ORecord<?>>) response);
		else if (response instanceof Integer)
			sendTextContent(iRequest, OHttpUtils.STATUS_OK_CODE, "OK", null, OHttpUtils.CONTENT_TEXT_PLAIN, response);
		else
			sendTextContent(iRequest, OHttpUtils.STATUS_OK_CODE, "OK", null, OHttpUtils.CONTENT_TEXT_PLAIN, response.toString());
	}

	public String[] getNames() {
		return NAMES;
	}
}
