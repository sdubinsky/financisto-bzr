/*******************************************************************************
 * Copyright 2010 Denis Solonenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ru.orangesoftware.orb;

import java.lang.reflect.Field;

class FieldInfo {
	
	final Field field;
	final String columnName;
	final FieldType type;
	final boolean required;
	final int index;
	
	private FieldInfo(int index, Field field, String columnName, FieldType type, boolean required) {
		this.index = index;
		this.field = field;
		this.columnName = columnName;
		this.type = type;
		this.required = required;
	}
	
	public static FieldInfo primitive(Field field, String columnName) {
		return new FieldInfo(0, field, columnName, FieldType.getType(field), false);
	}

	public static FieldInfo entity(int index, Field field, String columnName, boolean required) {
		return new FieldInfo(index, field, columnName, new FieldType.ENTITY(field.getType()), required);
	}

	@Override
	public String toString() {
		return "["+index+":"+columnName+","+type+"]";
	}

}
