package me.oscardoras.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {

	public static void main(String[] args) {
		File file = new File("servers");
		if (!file.exists()) file.mkdirs();
		
		Save.start();
		Update.start();
		
		Scanner scanner = new Scanner(System.in);
		String line;
		while (!(line = scanner.nextLine()).equals("leave")) {
			try {
				if (line.startsWith("proxy ") && line.length() >= 7) BungeeCord.send(line.substring(6));
				else if (line.startsWith("start ") && line.length() >= 7) {
					String server = line.substring(6);
					if (server.equals("proxy")) {
						if (!BungeeCord.isLoaded()) BungeeCord.load();
						else System.out.println("Proxy already loaded");
					} else {
						SubServer subServer = SubServer.getSubServer(server);
						if (subServer != null) {
							if (!subServer.isLoaded()) subServer.load();
							else System.out.println("Server already loaded");
						} else System.out.println("Server not found");
					}
				} else if (line.startsWith("stop ") && line.length() >= 6) {
					String server = line.substring(5);
					if (server.equals("proxy")) {
						if (BungeeCord.isLoaded()) BungeeCord.unload();
						else System.out.println("Proxy not loaded");
					} else {
						SubServer subServer = SubServer.getSubServer(server);
						if (subServer != null) {
							if (subServer.isLoaded()) subServer.unload();
							else System.out.println("Server not loaded");
						} else System.out.println("Server not found");
					}
				} else if (line.startsWith("kill ") && line.length() >= 6) {
					String server = line.substring(5);
					if (server.equals("proxy")) {
						if (BungeeCord.isLoaded()) BungeeCord.kill();
						else System.out.println("Proxy not loaded");
					} else {
						SubServer subServer = SubServer.getSubServer(server);
						if (subServer != null) {
							if (subServer.isLoaded()) subServer.kill();
							else System.out.println("Server not loaded");
						} else System.out.println("Server not found");
					}
				} else if (line.startsWith("save ") && line.length() >= 6) {
					SubServer subServer = SubServer.getSubServer(line.substring(5));
					if (subServer != null) Save.save(subServer);
					else System.out.println("Server not found");
				} else {
					boolean found = false;
					for (SubServer subServer : ServerStream.streams.keySet()) {
						String name = subServer.getName();
						if (line.startsWith(name + " ") && line.length() >= name.length() + 2) {
							ServerStream.streams.get(subServer).send(line.substring(name.length() + 1));
							found = true;
							break;
						}
					}
					if (!found) {
						System.out.println("Command not found");
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		scanner.close();
		
		BungeeCord.unload(() -> {
			SubServer.unloadServers(() -> {
				System.exit(0);
			});
		});
	}
	
	
	public static JsonElement getProperty(String serverName, String key, JsonElement defaultValue) {
		try {
			File file = new File("servers.json");
			file.setReadable(true);
			file.setWritable(true);
			if (!file.isFile()) {
				if (file.exists()) file.delete();
				file.mkdirs();
				file.delete();
				file.createNewFile();
			}
			JsonElement jsonElement = new JsonParser().parse(IOUtils.toString(new FileInputStream(file), "UTF-8"));
			
			boolean save = false;
			if (!jsonElement.isJsonObject()) {
				jsonElement = new JsonObject();
				save = true;
			}
			JsonObject servers = jsonElement.getAsJsonObject();
			if (!servers.has(serverName) || !servers.get(serverName).isJsonObject()) {
				servers.add(serverName, new JsonObject());
				save = true;
			}
			JsonObject properties = servers.get(serverName).getAsJsonObject();
			if (!properties.has(key)) {
				properties.add(key, defaultValue);
				save = true;
			}
			
			if (save) IOUtils.write(jsonElement.toString(), new FileOutputStream(file), "UTF-8");
			
			return properties.get(key);
		} catch (IOException ex) {
			ex.printStackTrace();
			return defaultValue;
		}
	}
	
}