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
package com.orientechnologies.orient.core.command;

/**
 * Generic GOF command pattern implementation.
 * 
 * @author Luca Garulli
 * 
 * @param <T>
 */
public interface OCommandExecutor {

	/**
	 * Parse the request. Once parsed the command can be executed multiple times by using the execute() method.
	 * 
	 * @param iRequest
	 *          Command request implementation.
	 * @param iArgs
	 *          Optional variable arguments to pass to the command.
	 * 
	 * @see #execute(Object...)
	 * @return
	 */
	public <RET extends OCommandExecutor> RET parse(OCommandRequestInternal iRequest);

	/**
	 * Execute the requested command parsed previously.
	 * 
	 * @param iArgs
	 *          Optional variable arguments to pass to the command.
	 * 
	 * @see #parse(OCommandRequestInternal)
	 * @return
	 */
	public Object execute(final Object... iArgs);

	/**
	 * Execute the requested command after parsing it.
	 * 
	 * @param iRequest
	 *          Command request implementation.
	 * @param iArgs
	 *          Optional variable arguments to pass to the command.
	 * 
	 * @see #execute(Object...)
	 * @return
	 */
	public Object execute(OCommandRequestInternal iRequest, final Object... iArgs);
}
