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
package com.orientechnologies.orient.core.sql.filter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.OPair;
import com.orientechnologies.orient.core.command.OCommandToParse;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;

/**
 * Represent an object field as value in the query condition.
 * 
 * @author Luca Garulli
 * 
 */
public abstract class OSQLFilterItemAbstract implements OSQLFilterItem {
	protected String															name;
	protected List<OPair<Integer, List<String>>>	operationsChain	= null;

	public OSQLFilterItemAbstract(final OCommandToParse iQueryToParse, final String iName) {
		int pos = iName.indexOf(OSQLFilterFieldOperator.CHAIN_SEPARATOR);
		if (pos > -1) {
			// GET ALL SPECIAL OPERATIONS
			name = iName.substring(0, pos);

			String part = iName;
			String partUpperCase = part.toUpperCase();
			boolean operatorFound;

			while (pos > -1) {
				part = part.substring(pos + OSQLFilterFieldOperator.CHAIN_SEPARATOR.length());
				partUpperCase = partUpperCase.substring(pos + OSQLFilterFieldOperator.CHAIN_SEPARATOR.length());

				operatorFound = false;
				for (OSQLFilterFieldOperator op : OSQLFilterFieldOperator.OPERATORS)
					if (partUpperCase.startsWith(op.keyword + "(")) {
						// OPERATOR MATCH
						final List<String> arguments;

						if (op.minArguments > 0) {
							arguments = OStringSerializerHelper.getParameters(part);
							if (arguments.size() < op.minArguments || arguments.size() > op.maxArguments)
								throw new OQueryParsingException(iQueryToParse.text, "Syntax error: field operator '" + op.keyword + "' needs "
										+ (op.minArguments == op.maxArguments ? op.minArguments : op.minArguments + "-" + op.maxArguments)
										+ " argument(s) while has been received " + arguments.size(), iQueryToParse.currentPos + pos);
						} else
							arguments = null;

						// SPECIAL OPERATION FOUND: ADD IT IN TO THE CHAIN
						if (operationsChain == null)
							operationsChain = new ArrayList<OPair<Integer, List<String>>>();

						operationsChain.add(new OPair<Integer, List<String>>(op.id, arguments));

						pos = partUpperCase.indexOf(OStringSerializerHelper.CLOSED_BRACE) + OSQLFilterFieldOperator.CHAIN_SEPARATOR.length();
						operatorFound = true;
						break;
					}

				if (!operatorFound) {
					pos = partUpperCase.indexOf(OSQLFilterFieldOperator.CHAIN_SEPARATOR, 0);

					// CHECK IF IT'S A FIELD
					int posOpenBrace = part.indexOf("(");
					if (posOpenBrace == -1 || posOpenBrace > pos && pos > -1) {
						// YES, SEEMS A FIELD
						String chainedFieldName = pos > -1 ? part.substring(0, pos) : part;

						if (operationsChain == null)
							operationsChain = new ArrayList<OPair<Integer, List<String>>>();

						final List<String> list = new ArrayList<String>();
						list.add(chainedFieldName);
						operationsChain.add(new OPair<Integer, List<String>>(OSQLFilterFieldOperator.FIELD.id, list));
					} else
						// ERROR: OPERATOR NOT FOUND OR MISPELLED
						throw new OQueryParsingException(iQueryToParse.text,
								"Syntax error: field operator not recognized between the supported ones: "
										+ Arrays.toString(OSQLFilterFieldOperator.OPERATORS), iQueryToParse.currentPos + pos);
				}

				if (pos >= partUpperCase.length())
					return;

				pos = partUpperCase.indexOf(OSQLFilterFieldOperator.CHAIN_SEPARATOR, pos);
			}
		} else
			name = iName;
	}

	public Object transformValue(final ODatabaseRecord<?> iDatabase, Object iResult) {
		if (iResult != null && operationsChain != null) {
			// APPLY OPERATIONS FOLLOWING THE STACK ORDER
			int operator = -2;

			try {
				for (OPair<Integer, List<String>> op : operationsChain) {
					operator = op.key.intValue();

					// NO ARGS OPERATORS
					if (operator == OSQLFilterFieldOperator.SIZE.id)
						iResult = iResult != null ? ((Collection<?>) iResult).size() : 0;

					else if (operator == OSQLFilterFieldOperator.LENGTH.id)
						iResult = iResult != null ? iResult.toString().length() : 0;

					else if (operator == OSQLFilterFieldOperator.TOUPPERCASE.id)
						iResult = iResult != null ? iResult.toString().toUpperCase() : 0;

					else if (operator == OSQLFilterFieldOperator.TOLOWERCASE.id)
						iResult = iResult != null ? iResult.toString().toLowerCase() : 0;

					else if (operator == OSQLFilterFieldOperator.TRIM.id)
						iResult = iResult != null ? iResult.toString().trim() : null;

					// OTHER OPERATORS
					else if (operator == OSQLFilterFieldOperator.FIELD.id) {
						if (iResult != null) {

							ODocument record;
							if (iResult instanceof String) {
								record = new ODocument(iDatabase, new ORecordId((String) iResult));
							} else if (iResult instanceof ORID)
								record = new ODocument(iDatabase, (ORID) iResult);
							else if (iResult instanceof ORecord<?>)
								record = (ODocument) iResult;
							else
								throw new IllegalArgumentException("Field " + name + " is not a ODocument object");

							try {
								record.load();
								iResult = iResult != null ? record.field(op.value.get(0)) : null;
							} catch (ORecordNotFoundException e) {
								iResult = null;
							}
						}

					} else if (operator == OSQLFilterFieldOperator.CHARAT.id) {
						int index = Integer.parseInt(op.value.get(0));
						iResult = iResult != null ? iResult.toString().substring(index, index + 1) : null;

					} else if (operator == OSQLFilterFieldOperator.INDEXOF.id && op.value.get(0).length() > 2) {
						String toFind = op.value.get(0).substring(1, op.value.get(0).length() - 1);
						int startIndex = op.value.size() > 1 ? Integer.parseInt(op.value.get(1)) : 0;
						iResult = iResult != null ? iResult.toString().indexOf(toFind, startIndex) : null;

					} else if (operator == OSQLFilterFieldOperator.SUBSTRING.id) {

						int endIndex = op.value.size() > 1 ? Integer.parseInt(op.value.get(1)) : op.value.get(0).length();
						iResult = iResult != null ? iResult.toString().substring(Integer.parseInt(op.value.get(0)), endIndex) : null;
					} else if (operator == OSQLFilterFieldOperator.FORMAT.id)

						iResult = iResult != null ? String.format(op.value.get(0), iResult) : null;

					else if (operator == OSQLFilterFieldOperator.LEFT.id) {
						final int len = Integer.parseInt(op.value.get(0));
						iResult = iResult != null ? iResult.toString().substring(0,
								len <= iResult.toString().length() ? len : iResult.toString().length()) : null;

					} else if (operator == OSQLFilterFieldOperator.RIGHT.id) {
						final int offset = Integer.parseInt(op.value.get(0));
						iResult = iResult != null ? iResult.toString().substring(
								offset <= iResult.toString().length() - 1 ? offset : iResult.toString().length() - 1) : null;

					} else if (operator == OSQLFilterFieldOperator.ASSTRING.id)
						iResult = iResult != null ? iResult.toString() : null;
					else if (operator == OSQLFilterFieldOperator.ASINTEGER.id)
						iResult = iResult != null ? new Integer(iResult.toString()) : null;
					else if (operator == OSQLFilterFieldOperator.ASFLOAT.id)
						iResult = iResult != null ? new Float(iResult.toString()) : null;
					else if (operator == OSQLFilterFieldOperator.ASBOOLEAN.id) {
						if (iResult != null) {
							if (iResult instanceof String)
								iResult = new Boolean((String) iResult);
							else if (iResult instanceof Number) {
								final int bValue = ((Number) iResult).intValue();
								if (bValue == 0)
									iResult = Boolean.FALSE;
								else if (bValue == 1)
									iResult = Boolean.TRUE;
								else
									// IGNORE OTHER VALUES
									iResult = null;
							}
						}
					} else if (operator == OSQLFilterFieldOperator.ASDATE.id) {
						if (iResult != null) {
							if (iResult instanceof Long)
								iResult = new Date((Long) iResult);
							else
								iResult = iDatabase.getStorage().getConfiguration().getDateFormatInstance().parse(iResult.toString());
						}
					} else if (operator == OSQLFilterFieldOperator.ASDATETIME.id) {
						if (iResult != null) {
							if (iResult instanceof Long)
								iResult = new Date((Long) iResult);
							else
								iResult = iDatabase.getStorage().getConfiguration().getDateTimeFormatInstance().parse(iResult.toString());
						}
					}
				}
			} catch (ParseException e) {
				OLogManager.instance().exception("Error on conversion of value '%s' using field operator %s", e,
						OCommandExecutionException.class, iResult, OSQLFilterFieldOperator.getById(operator));
			}
		}

		return iResult;
	}

	public boolean hasChainOperators() {
		return operationsChain != null;
	}

	@Override
	public String toString() {
		return name != null ? name : "";
	}

	public String getName() {
		return name;
	}
}
