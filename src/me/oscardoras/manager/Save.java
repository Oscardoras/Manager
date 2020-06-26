package me.oscardoras.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class Save {
	private Save() {}
	
	
	public static void start() {
		final long day = 86400000l;
		new Timer().schedule(new TimerTask() {
			public void run() {
				for (SubServer subServer : SubServer.getSubServers()) {
					JsonElement save = Main.getProperty(subServer.getName(), "save", new JsonPrimitive(true));
					if (save.isJsonPrimitive() && save.getAsBoolean()) {
						try {
							save(subServer);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		}, (new Date().getTime() + day) % day, day);
	}
	
	protected static File save(SubServer subServer) throws IOException {
		System.out.println("Saving server " + subServer.getName() + "...");
		File file = getFile(subServer);
        zip(subServer.getDirectory(), file);
        return file;
	}
	
	private static File getFile(SubServer subServer) throws IOException {
	    String name = "saves/" + subServer.getName() + "-" + new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
	    
	    File file;
	    for (int i = 1; (file = new File(name + "_" + i + ".zip")).exists(); i++);
	    
	    file.mkdirs();
	    file.delete();
		file.createNewFile();
	    return file;
    }
	
	private static void zip(File folder, File zip) throws IOException {
	    int folderLength = folder.getCanonicalPath().length() + 1;
	    
        List<File> list = new ArrayList<File>();
        listFiles(folder, list);
        
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zip));
        for (File file : list) {
            String path = file.getCanonicalPath();
            ZipEntry zipEntry = new ZipEntry(path.substring(folderLength, path.length()));
            zipOutputStream.putNextEntry(zipEntry);
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, len);
            }
            zipOutputStream.closeEntry();
            fileInputStream.close();
        }
        zipOutputStream.close();
    }
    
    private static void listFiles(File folder, List<File> list) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) listFiles(file, list);
            else list.add(file);
        }
    }
	
}