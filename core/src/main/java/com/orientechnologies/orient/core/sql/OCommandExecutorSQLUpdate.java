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
package com.orientechnologies.orient.core.sql;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.orientechnologies.common.parser.OStringParser;
import com.orientechnologies.common.util.OPair;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.core.record.ORecordSchemaAware;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItem;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;

/**
 * SQL UPDATE command.
 * 
 * @author Luca Garulli
 * 
 */
public class OCommandExecutorSQLUpdate extends OCommandExecutorSQLAbstract implements OCommandResultListener {
	public static final String									KEYWORD_UPDATE	= "UPDATE";
	private static final String									KEYWORD_SET			= "SET";
	private static final String									KEYWORD_ADD			= "ADD";
	private static final String									KEYWORD_PUT			= "PUT";
	private static final String									KEYWORD_REMOVE	= "REMOVE";
	private String															className				= null;
	private Map<String, Object>									setEntries			= new HashMap<String, Object>();
	private Map<String, Object>									addEntries			= new HashMap<String, Object>();
	private Map<String, OPair<String, Object>>	putEntries			= new HashMap<String, OPair<String, Object>>();
	private Map<String, Object>									removeEntries		= new HashMap<String, Object>();
	private OQuery<?>														query;
	private int																	recordCount			= 0;
	private static final Object									EMPTY_VALUE			= new Object();

	@SuppressWarnings("unchecked")
	public OCommandExecutorSQLUpdate parse(final OCommandRequestText iRequest) {
		iRequest.getDatabase().checkSecurity(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_UPDATE);

		init(iRequest.getDatabase(), iRequest.getText());

		className = null;
		setEntries.clear();
		query = null;
		recordCount = 0;

		final StringBuilder word = new StringBuilder();

		int pos = OSQLHelper.nextWord(text, textUpperCase, 0, word, true);
		if (pos == -1 || !word.toString().equals(OCommandExecutorSQLUpdate.KEYWORD_UPDATE))
			throw new OCommandSQLParsingException("Keyword " + OCommandExecutorSQLUpdate.KEYWORD_UPDATE + " not found", text, 0);

		int newPos = OSQLHelper.nextWord(text, textUpperCase, pos, word, true);
		if (newPos == -1)
			throw new OCommandSQLParsingException("Invalid cluster/class name", text, pos);

		pos = newPos;

		String subjectName = word.toString();

		if (subjectName.startsWith(OCommandExecutorSQLAbstract.CLASS_PREFIX))
			subjectName = subjectName.substring(OCommandExecutorSQLAbstract.CLASS_PREFIX.length());

		// CLASS
		final OClass cls = database.getMetadata().getSchema().getClass(subjectName);
		if (cls == null)
			throw new OCommandSQLParsingException("Class " + subjectName + " not found in database", text, pos);

		className = cls.getName();

		newPos = OSQLHelper.nextWord(text, textUpperCase, pos, word, true);
		if (newPos == -1
				|| (!word.toString().equals(KEYWORD_SET) && !word.toString().equals(KEYWORD_ADD) && !word.toString().equals(KEYWORD_PUT) && !word
						.toString().equals(KEYWORD_REMOVE)))
			throw new OCommandSQLParsingException("Expected keyword " + KEYWORD_SET + "," + KEYWORD_ADD + "," + KEYWORD_PUT + " or "
					+ KEYWORD_REMOVE, text, pos);

		pos = newPos;

		while (pos != -1 && !word.toString().equals(OCommandExecutorSQLAbstract.KEYWORD_WHERE)) {
			if (word.toString().equals(KEYWORD_SET))
				pos = parseSetFields(word, pos);
			else if (word.toString().equals(KEYWORD_ADD))
				pos = parseAddFields(word, pos);
			else if (word.toString().equals(KEYWORD_PUT))
				pos = parsePutFields(word, pos);
			else if (word.toString().equals(KEYWORD_REMOVE))
				pos = parseRemoveFields(word, pos);
			else
				break;
		}

		String whereCondition = word.toString();

		if (whereCondition.equals(OCommandExecutorSQLAbstract.KEYWORD_WHERE))
			query = new OSQLAsynchQuery<ODocument>("select from " + className + " where " + text.substring(pos), this);
		else
			query = new OSQLAsynchQuery<ODocument>("select from " + className, this);

		return this;
	}

	public Object execute(final Object... iArgs) {
		if (className == null)
			throw new OCommandExecutionException("Can't execute the command because it hasn't been parsed yet");

		database.query(query);
		return recordCount;
	}

	/**
	 * Update current record.
	 */
	@SuppressWarnings("unchecked")
	public boolean result(final Object iRecord) {
		ORecordSchemaAware<?> record = (ORecordSchemaAware<?>) iRecord;

		// BIND VALUES TO UPDATE
		Object v;
		for (Map.Entry<String, Object> entry : setEntries.entrySet()) {
			v = entry.getValue();

			if (v instanceof OSQLFilterItem)
				v = ((OSQLFilterItem) v).getValue(record);

			record.field(entry.getKey(), v);
		}

		// BIND VALUES TO ADD
		Collection<Object> coll;
		Object fieldValue;
		for (Map.Entry<String, Object> entry : addEntries.entrySet()) {
			fieldValue = record.field(entry.getKey());

			if (fieldValue instanceof Collection<?>) {
				coll = (Collection<Object>) fieldValue;

				v = entry.getValue();

				if (v instanceof OSQLFilterItem)
					v = ((OSQLFilterItem) v).getValue(record);

				coll.add(v);
				record.setDirty();
			}
		}

		// BIND VALUES TO PUT (IN COLLECTION)
		Map<String, Object> map;
		OPair<String, Object> pair;
		for (Entry<String, OPair<String, Object>> entry : putEntries.entrySet()) {
			fieldValue = record.field(entry.getKey());

			if (fieldValue instanceof Map<?, ?>) {
				map = (Map<String, Object>) fieldValue;

				pair = entry.getValue();

				if (pair.getValue() instanceof OSQLFilterItem)
					pair.setValue(((OSQLFilterItem) pair.getValue()).getValue(record));

				map.put(pair.getKey(), pair.getValue());
				record.setDirty();
			}
		}
		// REMOVE FIELD IF ANY
		for (Map.Entry<String, Object> entry : removeEntries.entrySet()) {
			v = entry.getValue();
			if (v == EMPTY_VALUE)
				record.removeField(entry.getKey());
			else {
				fieldValue = record.field(entry.getKey());

				if (fieldValue instanceof Collection<?>) {
					coll = (Collection<Object>) fieldValue;
					coll.remove(v);
					record.setDirty();
				}
			}
		}

		record.save();
		recordCount++;
		return true;
	}

	private int parseSetFields(final StringBuilder word, int pos) {
		String fieldName;
		String fieldValue;
		int newPos = pos;

		while (pos != -1 && (setEntries.size() == 0 || word.toString().equals(","))) {
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos, word, false);
			if (newPos == -1)
				throw new OCommandSQLParsingException("Field name expected", text, pos);
			pos = newPos;

			fieldName = word.toString();

			newPos = OStringParser.jumpWhiteSpaces(text, pos);

			if (newPos == -1 || text.charAt(newPos) != '=')
				throw new OCommandSQLParsingException("Character '=' was expected", text, pos);

			pos = newPos;
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos + 1, word, false, " =><");
			if (pos == -1)
				throw new OCommandSQLParsingException("Value expected", text, pos);

			fieldValue = word.toString();

			if (fieldValue.endsWith(",")) {
				pos = newPos - 1;
				fieldValue = fieldValue.substring(0, fieldValue.length() - 1);
			} else
				pos = newPos;

			// INSERT TRANSFORMED FIELD VALUE
			setEntries.put(fieldName, OSQLHelper.parseValue(database, this, fieldValue));

			pos = OSQLHelper.nextWord(text, textUpperCase, pos, word, true);
		}

		if (setEntries.size() == 0)
			throw new OCommandSQLParsingException("Entries to set <field> = <value> are missed. Example: name = 'Bill', salary = 300.2",
					text, pos);

		return pos;
	}

	private int parseAddFields(final StringBuilder word, int pos) {
		String fieldName;
		String fieldValue;
		int newPos = pos;

		while (pos != -1 && (setEntries.size() == 0 || word.toString().equals(","))) {
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos, word, false);
			if (newPos == -1)
				throw new OCommandSQLParsingException("Field name expected", text, pos);
			pos = newPos;

			fieldName = word.toString();

			newPos = OStringParser.jumpWhiteSpaces(text, pos);

			if (newPos == -1 || text.charAt(newPos) != '=')
				throw new OCommandSQLParsingException("Character '=' was expected", text, pos);

			pos = newPos;
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos + 1, word, false, " =><");
			if (pos == -1)
				throw new OCommandSQLParsingException("Value expected", text, pos);

			fieldValue = word.toString();

			if (fieldValue.endsWith(",")) {
				pos = newPos - 1;
				fieldValue = fieldValue.substring(0, fieldValue.length() - 1);
			} else
				pos = newPos;

			// INSERT TRANSFORMED FIELD VALUE
			addEntries.put(fieldName, OSQLHelper.parseValue(database, this, fieldValue));

			pos = OSQLHelper.nextWord(text, textUpperCase, pos, word, true);
		}

		if (addEntries.size() == 0)
			throw new OCommandSQLParsingException("Entries to add <field> = <value> are missed. Example: name = 'Bill', salary = 300.2",
					text, pos);

		return pos;
	}

	private int parsePutFields(final StringBuilder word, int pos) {
		String fieldName;
		String fieldKey;
		String fieldValue;
		int newPos = pos;

		while (pos != -1 && (setEntries.size() == 0 || word.toString().equals(","))) {
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos, word, false);
			if (newPos == -1)
				throw new OCommandSQLParsingException("Field name expected", text, pos);
			pos = newPos;

			fieldName = word.toString();

			newPos = OStringParser.jumpWhiteSpaces(text, pos);

			if (newPos == -1 || text.charAt(newPos) != '=')
				throw new OCommandSQLParsingException("Character '=' was expected", text, pos);

			pos = newPos;
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos + 1, word, false, " =><");
			if (pos == -1)
				throw new OCommandSQLParsingException("Key expected", text, pos);

			fieldKey = word.toString();

			if (fieldKey.endsWith(",")) {
				pos = newPos + 1;
				fieldKey = fieldKey.substring(0, fieldKey.length() - 1);
			} else {
				pos = newPos;

				newPos = OStringParser.jumpWhiteSpaces(text, pos);
				if (newPos == -1 || text.charAt(pos) != ',')
					throw new OCommandSQLParsingException("',' expected", text, pos);

				pos = newPos;
			}

			newPos = OSQLHelper.nextWord(text, textUpperCase, pos + 1, word, false, " =><");
			if (pos == -1)
				throw new OCommandSQLParsingException("Value expected", text, pos);

			fieldValue = word.toString();

			if (fieldValue.endsWith(",")) {
				pos = newPos - 1;
				fieldValue = fieldValue.substring(0, fieldValue.length() - 1);
			} else
				pos = newPos;

			// INSERT TRANSFORMED FIELD VALUE
			putEntries.put(
					fieldName,
					new OPair<String, Object>((String) OSQLHelper.parseValue(database, this, fieldKey), OSQLHelper.parseValue(database, this,
							fieldValue)));

			pos = OSQLHelper.nextWord(text, textUpperCase, pos, word, true);
		}

		if (putEntries.size() == 0)
			throw new OCommandSQLParsingException("Entries to put <field> = <key>, <value> are missed. Example: name = 'Bill', 30", text,
					pos);

		return pos;
	}

	private int parseRemoveFields(final StringBuilder word, int pos) {
		String fieldName;
		String fieldValue;
		Object value;
		int newPos = pos;

		while (pos != -1 && (removeEntries.size() == 0 || word.toString().equals(","))) {
			newPos = OSQLHelper.nextWord(text, textUpperCase, pos, word, false);
			if (newPos == -1)
				throw new OCommandSQLParsingException("Field name expected", text, pos);

			fieldName = word.toString();

			pos = OStringParser.jumpWhiteSpaces(text, newPos);

			if (pos > -1 && text.charAt(pos) == '=') {
				pos = OSQLHelper.nextWord(text, textUpperCase, pos + 1, word, false, " =><");
				if (pos == -1)
					throw new OCommandSQLParsingException("Value expected", text, pos);

				fieldValue = word.toString();

				if (fieldValue.endsWith(",")) {
					pos = newPos - 1;
					fieldValue = fieldValue.substring(0, fieldValue.length() - 1);
				} else
					pos = newPos;

				if (fieldValue.length() > 2 && Character.isDigit(fieldValue.charAt(0)) && fieldValue.contains(":"))
					value = new ORecordId(fieldValue);
				else
					value = fieldValue;

			} else
				value = EMPTY_VALUE;

			// INSERT FIELD NAME TO BE REMOVED
			removeEntries.put(fieldName, value);

			pos = OSQLHelper.nextWord(text, textUpperCase, pos, word, true);
		}

		if (removeEntries.size() == 0)
			throw new OCommandSQLParsingException("Field(s) to remove are missed. Example: name, salary", text, pos);
		return pos;
	}
}
