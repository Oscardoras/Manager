package me.oscardoras.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BungeeCord {
	
	private static BungeeCord bungeeCord = null;
	
	protected final Process process;
	protected final PrintWriter out;
	protected final Thread shutdownHook;
	protected final List<Runnable> runnables = new ArrayList<Runnable>();
	
	private BungeeCord() throws IOException {
		List<String> args = new ArrayList<String>();
		args.add("java");
		JsonElement memory = Main.getProperty("proxy", "memory", new JsonPrimitive(256));
		int m = memory.isJsonPrimitive() && memory.getAsJsonPrimitive().isNumber() ? memory.getAsInt() : 256;
		args.add("-Xmx" + m + "M");
		args.add("-Xms" + (m <= 1024 ? m : 1024) + "M");
		args.add("-Djline.terminal=jline.UnsupportedTerminal");
		args.add("-DIReallyKnowWhatIAmDoingISwear");
		JsonElement flags = Main.getProperty("proxy", "flags", new JsonArray());
		if (flags.isJsonArray()) for (JsonElement flag : flags.getAsJsonArray()) if (flag.isJsonPrimitive() && flag.getAsJsonPrimitive().isString()) args.add(flag.getAsString());
		args.add("-jar");
		args.add("bungeecord.jar");
		this.process = new ProcessBuilder(args).start();
		
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
					if (line.length() > 0 && !line.startsWith(">")) System.out.println("[proxy] " + line);
				}
			} catch (IOException e) {
			} catch (Exception e) {
			    e.printStackTrace();
			}
		}).start();
		
		new Thread(() -> {
			try {
			    BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			    String line;
				while ((line = in.readLine()) != null) {
					if (line.length() > 0 && !line.startsWith(">")) System.out.println("[proxy] " + line);
				}
			} catch (IOException e) {
			} catch (Exception ex) {
			    ex.printStackTrace();
			}
		}).start();
		
		new Timer().schedule(new TimerTask() {
			public void run() {
				if (!process.isAlive()) {
					bungeeCord = null;
					out.close();
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
					this.cancel();
					
					System.out.println("...proxy stoped");
					for (Runnable runnable : runnables) runnable.run();
				}
			}
		}, 1l, 1l);
		
		bungeeCord = this;
	}
	
	public static void load() {
		if (!isLoaded()) {
			if (!new File("bungeecord.jar").isFile()) Update.updateBungee();
			System.out.println("Starting proxy...");
			try {
				new BungeeCord();
			} catch (Exception ex) {
				System.out.println("An error occurred while starting the proxy");
				ex.printStackTrace();
			}
		}
	}
	
	public static void unload(Runnable... runnables) {
		if (isLoaded()) {
			for (Runnable runnable : runnables) bungeeCord.runnables.add(runnable);
			send("end");
		} else for (Runnable runnable : runnables) runnable.run();
	}
	
	public static void kill() {
		if (isLoaded()) bungeeCord.process.destroyForcibly();
	}
	
	public static boolean isLoaded() {
		return bungeeCord != null && bungeeCord.process.isAlive();
	}
	
	public static void send(String line) {
		if (isLoaded()) {
			bungeeCord.out.println(line);
			bungeeCord.out.flush();
		}
	}
	
}