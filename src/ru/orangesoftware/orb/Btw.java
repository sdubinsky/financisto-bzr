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

import java.util.ArrayList;

public class Btw implements Expression {

	private final String field;
	private final Object value1;
	private final Object value2;
	
	Btw(String field, Object value1, Object value2) {
		this.field = field;
		this.value1 = value1;
		this.value2 = value2;
	}
	
	@Override
	public Selection toSelection(EntityDefinition ed) {
		ArrayList<String> args = new ArrayList<String>();
		args.add(String.valueOf(value1));
		args.add(String.valueOf(value2));
		return new Selection("("+ed.getColumnForField(field)+" between ? and ?)", args);
	}

}
