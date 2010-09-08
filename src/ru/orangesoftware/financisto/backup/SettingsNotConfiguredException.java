/* Copyright (c) 2008 Google Inc.
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


package ru.orangesoftware.financisto.backup;

/**
 * Exce��o usada quando ocorre algum erro relacionado aos parametros de configura��o do usu�rio 
 * */
public class SettingsNotConfiguredException extends Exception {
	
	public static final long serialVersionUID = 1;
	
	public SettingsNotConfiguredException() {
		super();
	}
	
	public SettingsNotConfiguredException(String msg) {
		super(msg);
	}
}