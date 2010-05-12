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
package com.orientechnologies.orient.server.network.protocol.http;

import java.io.IOException;
import java.net.Socket;

import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.server.OClientConnection;
import com.orientechnologies.orient.server.network.protocol.http.command.delete.OServerCommandDeleteDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetClass;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetCluster;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDictionary;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetQuery;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetServer;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStaticContent;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostStudioDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.put.OServerCommandPutDocument;

public class ONetworkProtocolHttpDb extends ONetworkProtocolHttpAbstract {
	private static final String	ORIENT_SERVER_DB	= "OrientDB Server v." + OConstants.ORIENT_VERSION;

	@Override
	public void config(Socket iSocket, OClientConnection iConnection) throws IOException {
		setName("HTTP-DB");
		super.config(iSocket, iConnection);
		data.serverInfo = ORIENT_SERVER_DB;

		registerCommand(new OServerCommandGetClass());
		registerCommand(new OServerCommandGetCluster());
		registerCommand(new OServerCommandGetDatabase());
		registerCommand(new OServerCommandGetDictionary());
		registerCommand(new OServerCommandGetDocument());
		registerCommand(new OServerCommandGetQuery());
		registerCommand(new OServerCommandGetServer());
		registerCommand(new OServerCommandGetStaticContent());

		registerCommand(new OServerCommandPostDocument());
		registerCommand(new OServerCommandPostStudioDocument());

		registerCommand(new OServerCommandPutDocument());

		registerCommand(new OServerCommandDeleteDocument());
	}
}
