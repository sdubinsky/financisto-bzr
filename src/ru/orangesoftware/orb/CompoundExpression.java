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
import java.util.Arrays;
import java.util.LinkedList;

class CompoundExpression implements Expression {

	private final String op;
	private final LinkedList<Expression> expressions = new LinkedList<Expression>();

	CompoundExpression(String op, Expression e) {
		this.op = op;
		this.expressions.add(e);
	}

	CompoundExpression(String op, Expression... e) {
		this.op = op;
		this.expressions.addAll(Arrays.asList(e));
	}

	@Override
	public Selection toSelection(EntityDefinition ed) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		ArrayList<String> list = new ArrayList<String>();
		boolean first = true;
		for (Expression e : expressions) {
			if (!first) {
				sb.append(" ").append(op).append(" ");				
			}
			Selection s = e.toSelection(ed); 
			sb.append(s.selection);
			list.addAll(s.selectionArgs);
			first = false;
		}
		sb.append(")");
		return new Selection(sb.toString(), list);
	}

}
