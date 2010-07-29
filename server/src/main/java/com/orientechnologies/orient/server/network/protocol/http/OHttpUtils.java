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

/**
 * Contains HTTP utilities static methods and constants.
 * 
 * @author Luca Garulli
 * 
 */
public class OHttpUtils {

	public static final String	URL_SEPARATOR							= "/";
	public static final char		URL_SEPARATOR_CHAR				= '/';
	public static final byte[]	EOL												= { (byte) '\r', (byte) '\n' };

	public static final String	METHOD_GET								= "GET";
	public static final String	METHOD_PUT								= "PUT";
	public static final String	METHOD_DELETE							= "DELETE";
	public static final String	METHOD_POST								= "POST";

	public static final String	CONTENT_LENGTH						= "CONTENT-LENGTH: ";

	public static final String	CONTENT_TEXT_PLAIN				= "text/plain";
	public static final String	CONTENT_JSON							= "application/json";

	public static final int			STATUS_OK_CODE						= 200;
	public static final String	STATUS_OK_DESCRIPTION			= "OK";
	public static final int			STATUS_ERROR							= 500;
	public static final String	STATUS_ERROR_DESCRIPTION	= "Error";
	public static final int			STATUS_AUTH_CODE					= 401;
	public static final String	STATUS_AUTH_DESCRIPTION		= "Unauthorized";

	public static String[] getParts(String iURI) {
		if (iURI == null || iURI.length() == 0)
			return new String[0];

		if (iURI.charAt(0) == URL_SEPARATOR_CHAR)
			iURI = iURI.substring(1);

		return iURI.split(URL_SEPARATOR);
	}
}
