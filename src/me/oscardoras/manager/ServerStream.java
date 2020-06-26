package me.oscardoras.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ServerStream {
	
	public static Map<SubServer, ServerStream> streams = new HashMap<SubServer, ServerStream>();
	
	public final SubServer subServer;
	public final boolean mainJar;
	protected final Process process;
	protected final PrintWriter out;
	protected final Thread shutdownHook;
	protected final List<Runnable> runnables = new ArrayList<Runnable>();
	
	
	public ServerStream(SubServer subServer) throws IOException {
		this.subServer = subServer;
		this.mainJar = subServer.getJar() == null;
		
		Main.getProperty(subServer.getName(), "autoRestart", new JsonPrimitive(false));
		Main.getProperty(subServer.getName(), "save", new JsonPrimitive(true));
		
		List<String> args = new ArrayList<String>();
		args.add("java");
		JsonElement memory = Main.getProperty(subServer.getName(), "memory", new JsonPrimitive(1024));
		int m = memory.isJsonPrimitive() && memory.getAsJsonPrimitive().isNumber() ? memory.getAsInt() : 1024;
		args.add("-Xmx" + m + "M");
		args.add("-Xms" + (m <= 1024 ? m : 1024) + "M");
		args.add("-DIReallyKnowWhatIAmDoingISwear");
		JsonElement flags = Main.getProperty(subServer.getName(), "flags", new JsonArray());
		if (flags.isJsonArray()) for (JsonElement flag : flags.getAsJsonArray()) if (flag.isJsonPrimitive() && flag.getAsJsonPrimitive().isString()) args.add(flag.getAsString());
		args.add("-jar");
		args.add(mainJar ? "../../spigot.jar" : "server.jar");
		args.add("nogui");
		ProcessBuilder processBuilder = new ProcessBuilder(args);
		processBuilder.directory(subServer.getDirectory());
		this.process = processBuilder.start();
		
		this.out = new PrintWriter(process.getOutputStream());
		
		this.shutdownHook = new Thread(() -> {
			process.destroy();
		});
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		
		new Thread(() -> {
			try {
			    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			    String line;
				while ((line = in.readLine()) != null) {
					if (line.length() > 0) System.out.println("[" + subServer.getName() + "] " + line);
				}
			} catch (IOException e) {
			} catch (Exception ex) {
			    ex.printStackTrace();
			}
		}).start();
		
		new Thread(() -> {
				try {
			    BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			    String line;
				while ((line = in.readLine()) != null) {
					if (line.length() > 0) System.out.println("[" + subServer.getName() + "] " + line);
				}
			} catch (IOException e) {
			} catch (Exception ex) {
			    ex.printStackTrace();
			}
		}).start();
		
		new Timer().schedule(new TimerTask() {
			public void run() {
				if (!process.isAlive()) {
					streams.remove(subServer);
					out.close();
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
					this.cancel();
					
					System.out.println("..." + subServer.getName() + " stoped");
					if (!runnables.isEmpty()) for (Runnable runnable : runnables) runnable.run();
					else {
						JsonElement autoRestart = Main.getProperty(subServer.getName(), "autoRestart", new JsonPrimitive(false));
						if (autoRestart.isJsonPrimitive() && autoRestart.getAsJsonPrimitive().isBoolean() && autoRestart.getAsBoolean()) subServer.load();
					}
				}
			}
		}, 1L, 1L);
		
		streams.put(this.subServer, this);
	}
	
	public void send(String line) {
		if (process.isAlive()) {
			out.println(line);
			out.flush();
		}
	}
	
	public void stop(Runnable... runnables) {
		this.runnables.add(() -> {});
		for (Runnable runnable : runnables) this.runnables.add(runnable);
		send("stop");
	}
	
	public void kill() {
		if (process.isAlive()) {
			runnables.add(() -> {});
			process.destroyForcibly();
		}
	}
	
}