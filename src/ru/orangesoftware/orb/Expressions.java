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

public class Expressions {
	
	private Expressions() {}
	
	public static Expression eq(String field, Object value) {
		return new Eq(field, value);
	}
	
	public static Expression neq(String field, Object value) {
		return new Neq(field, value);
	}

	public static Expression not(Expression e) {
		return new Not(e);
	}

	public static Expression and(Expression e) {
		return new And(e);
	}
	
	public static Expression and(Expression... ee) {
		return new And(ee);
	}
	
	public static Expression or(Expression e1, Expression e2) {
		return new Or(e1, e2);
	}
	
	public static Expression or(Expression... ee) {
		return new Or(ee);
	}

	public static Expression lte(String field, Object value) {
		return new Lte(field, value);
	}

	public static Expression gte(String field, Object value) {
		return new Gte(field, value);
	}

	public static Expression lt(String field, Object value) {
		return new Lt(field, value);
	}

	public static Expression gt(String field, Object value) {
		return new Gt(field, value);
	}

	public static Expression btw(String field, Object value1, Object value2) {
		return new Btw(field, value1, value2);
	}
}

