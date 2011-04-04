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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.orangesoftware.financisto.backup.SettingsNotConfiguredException;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import api.wireless.gdata.docs.client.DocsClient;
import api.wireless.gdata.docs.data.DocumentEntry;
import api.wireless.gdata.docs.data.FolderEntry;

public abstract class Export {
	
	public static final File EXPORT_PATH =  new File(Environment.getExternalStorageDirectory(), "financisto");

	public String export() throws Exception {
		File path = getPath();
		path.mkdirs();
        String fileName = generateFilename();
        File file = new File(path, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            export(outputStream);
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        return fileName;
	}

    protected void export(OutputStream outputStream) throws Exception {
        generateBackup(outputStream);
    }
	
	/**
	 * Backup database to google docs
	 * 
	 * @param docsClient Google docs connection
	 * @param folder Google docs folder name 
	 * */
	public String exportOnline(DocsClient docsClient, String folder) throws Exception {
		// check folder first
		FolderEntry fd = docsClient.getFolderByTitle(folder);
		if (fd == null) {
			throw new SettingsNotConfiguredException("folder-not-found");
		}

		// generation backup file
		String fileName = generateFilename();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		generateBackup(outputStream);
		
		// transforming streams
		InputStream backup = new ByteArrayInputStream(outputStream.toByteArray()); 
		
		// creating document on Google Docs
		DocumentEntry entry = new DocumentEntry();
		entry.setTitle(fileName);
		docsClient.createDocumentInFolder(entry, backup, "text/plain",fd.getKey());
		
		return fileName;
	}
	
	private String generateFilename() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
		return df.format(new Date())+getExtension();
	}
	
	private void generateBackup(OutputStream outputStream) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw, 65536);
		//writing utf bom into file
		byte[] bom = new byte[3];
		bom[0] = (byte) 0xEF;
		bom[1] = (byte) 0xBB;
		bom[2] = (byte) 0xBF;
		bw.write(new String(bom,"UTF-8"));
		
		try {			
			writeHeader(bw);
			writeBody(bw);
			writeFooter(bw);
		} finally {
			bw.close();
		}	
	}

	protected abstract  void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException;

	protected abstract  void writeBody(BufferedWriter bw) throws IOException;

	protected abstract  void writeFooter(BufferedWriter bw) throws IOException;

	protected abstract String getExtension();
	
	protected File getPath() {
		return EXPORT_PATH;
	}

}
