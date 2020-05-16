package org.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubServer {
	
	protected final File directory;
	
	protected SubServer(File directory) {
	    this.directory = directory;
	}
	
	public File getDirectory() {
		return directory;
	}
	
	public String getName() {
		return directory.getName();
	}
	
	public File getJar() {
		File file = new File("servers/" + getName() + "/server.jar");
		if (file.isFile()) return file;
		else return null;
	}
	
	public void load() {
		if (!isLoaded()) {
			if (!new File("spigot.jar").isFile()) Update.updateSpigot();
			System.out.println("Starting " + getName() + "...");
			try {
				new ServerStream(this);
			} catch (Exception ex) {
				System.out.println("An error occurred while starting the server " + getName());
				ex.printStackTrace();
			}
		}
	}
	
	
	public void unload(Runnable... runnables) {
		if (isLoaded()) ServerStream.streams.get(this).stop(runnables);
		else for (Runnable runnable : runnables) runnable.run();
	}
	
	public void kill() {
		if (isLoaded()) ServerStream.streams.get(this).kill();
	}
	
	public boolean isLoaded() {
		return ServerStream.streams.keySet().contains(this) && ServerStream.streams.get(this).process.isAlive();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object != null) {
			if (object instanceof SubServer) return getName().equals(((SubServer) object).getName());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}
	
	
	static public SubServer getSubServer(String name) {
	    if (!name.equals("proxy") && !name.equals("start") && !name.equals("stop") && !name.equals("kill") && !name.equals("save") && !name.equals("leave")) {
	    	File file = new File("servers/" + name);
	    	if (file.isDirectory()) return new SubServer(file);
	    }
	    return null;
	}
	
	static public List<SubServer> getSubServers() {
		List<SubServer> list = new ArrayList<SubServer>();
		File servers = new File("servers");
		servers.mkdirs();
		for (File file : servers.listFiles()) {
			SubServer subServer = SubServer.getSubServer(file.getName());
			if (subServer != null) list.add(subServer);
		}
		return list;
	}
	
	static public void unloadServers(Runnable... runnables) {
		class Mod {
			int toStop = 0;
		}
		Mod mod = new Mod();
		for (ServerStream serverStream : ServerStream.streams.values()) {
		    if (serverStream.mainJar) {
		        mod.toStop++;
				serverStream.stop(() -> {
					mod.toStop--;
					if (mod.toStop == 0) for (Runnable runnbale : runnables) runnbale.run();
				});
		    }
		}
		if (mod.toStop == 0) for (Runnable runnbale : runnables) runnbale.run();
	}
	
}