/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Export {
	
	public static final String EXPORT_PATH = "/sdcard/financisto/";

	public String export() throws Exception {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
		String fileName = df.format(new Date())+getExtension();
		File path = new File(getPath());
		path.mkdirs();
		File file = new File(path, fileName);		
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw, 65536);
		try {			
			writeHeader(bw);
			writeBody(bw);
			writeFooter(bw);
			return fileName;
		} finally {
			bw.close();
		}		
	}

	protected abstract  void writeHeader(BufferedWriter bw) throws Exception;

	protected abstract  void writeBody(BufferedWriter bw) throws Exception;

	protected abstract  void writeFooter(BufferedWriter bw) throws Exception;

	protected abstract String getExtension();
	
	protected String getPath() {
		return EXPORT_PATH;
	}

}
