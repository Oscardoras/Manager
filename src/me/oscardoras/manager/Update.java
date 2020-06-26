package me.oscardoras.manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class Update {
	private Update() {}
	
	private static final String bungeeTestUrl = "https://ci.md-5.net/job/BungeeCord/%build%/api/json";
	private static final String bungeeUrl = "https://ci.md-5.net/job/BungeeCord/%build%/artifact/bootstrap/target/BungeeCord.jar";
	private static final String spigotUrl = "https://cdn.getbukkit.org/spigot/spigot-%version%.jar";
	private static final String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
	private static boolean updatingBungee = false;
	private static boolean updatingSpigot = false;
	private static TimerTask task = new TimerTask() {
		public void run() {
			updateBungee();
			updateSpigot();
		}
	};
	
	public static void start() {
		System.out.println("Looking for updates...");
		updateBungee();
		updateSpigot();
		System.out.println("BungeeCord and Spigot up to date");
		new Timer().schedule(task, 5*60*1000l, 5*60*1000l);
	}
	
	public static void stop() {
		if (task != null) task.cancel();
	}
	
	private static String getSpigotVersion() throws IOException {
		File file = new File("spigot.jar");
		if (file.isFile()) {
			JarFile jarfile = new JarFile(file);
			JarEntry entry = jarfile.getJarEntry("META-INF/maven/org.spigotmc/spigot/pom.properties");
			Properties properties = new Properties();
			properties.load(jarfile.getInputStream(entry));
			jarfile.close();
			return properties.getProperty("version").split("-")[0];
		}
		return "1.13";
	}
	
	private static int getBungeeBuild() throws IOException {
		File file = new File("bungeecord.jar");
		if (file.isFile()) {
			JarFile jarfile = new JarFile(file);
			JarEntry entry = jarfile.getJarEntry("META-INF/MANIFEST.MF");
			String content = IOUtils.toString(jarfile.getInputStream(entry), "UTF-8");
			jarfile.close();
			return Integer.valueOf(content.substring(content.indexOf("Implementation-Version")).split(":")[5].split("\r\n")[0]);
		}
		return 1400;
	}
	
	public static void updateSpigot() {
		if (!updatingSpigot) {
			updatingSpigot = true;
			try {
				final String version = getSpigotVersion();
				
				String lastVersion = version;
				String[] numbers = lastVersion.split("\\.");
				int s = Integer.parseInt(numbers[1]);
				for (int second = s; second < 20; second++) {
					int fird = 0;
					if (second == s) {
						try {
							fird = Integer.parseInt(numbers[2]) + 1;
						} catch (Exception ex) {}
					}
					for (; fird < 20; fird++) {
						String v = "1." + second + (fird != 0 ? ("." + fird) : "");
						try {
							HttpURLConnection connection = ((HttpURLConnection) new URL(spigotUrl.replaceAll("%version%", v)).openConnection());
							connection.setRequestProperty("User-Agent", userAgent);
							if (connection.getResponseCode() == 200) {
								String contentType = connection.getHeaderField("Content-Type");
								if (contentType != null && contentType.equals("application/java-archive")) lastVersion = v;
							}
						} catch (IOException ex) {}
					}
					if (!lastVersion.startsWith("1." + second)) break;
				}
				
				if (!lastVersion.equals(version)) {
					final String v = lastVersion;
					final List<SubServer> subServers = new ArrayList<SubServer>();
					
					Runnable runnable = () -> {
						System.out.println("Updating Spigot to " + v + "...");
						try {
							HttpURLConnection connection = ((HttpURLConnection) new URL(spigotUrl.replaceAll("%version%", v)).openConnection());
							connection.setRequestProperty("User-Agent", userAgent);
							if (connection.getResponseCode() == 200)
								IOUtils.copy(connection.getInputStream(), new FileOutputStream("spigot.jar"));
							System.out.println("Spigot updated");
						} catch (IOException ex) {
							System.out.println("Spigot update to " + v + " failed");
							ex.printStackTrace();
						}
						for (SubServer subServer : subServers) subServer.load();
					};
					
				    class Mod {
						int toStop = 0;
					}
					Mod mod = new Mod();
					for (ServerStream serverStream : ServerStream.streams.values()) {
					    if (serverStream.mainJar) {
					    	if (serverStream.subServer.isLoaded()) subServers.add(serverStream.subServer);
					        mod.toStop++;
							serverStream.stop(() -> {
								mod.toStop--;
								if (mod.toStop == 0) runnable.run();
							});
					    }
					}
					if (mod.toStop == 0) runnable.run();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			updatingSpigot = false;
		}
	}
	
	public static void updateBungee() {
		if (!updatingBungee) {
			updatingBungee = true;
			try {
				int build = getBungeeBuild();
				int lastBuild = build;
				for (int b = lastBuild;; b++) {
					try {
						HttpURLConnection connection = ((HttpURLConnection) new URL(bungeeTestUrl.replaceAll("%build%", ""+b)).openConnection());
						connection.setRequestProperty("User-Agent", userAgent);
						if (connection.getResponseCode() == 200) {
							JsonElement jsonElement = new JsonParser().parse(IOUtils.toString(connection.getInputStream(), "UTF-8"));
							if (jsonElement.isJsonObject()) {
								JsonObject jsonObject = jsonElement.getAsJsonObject();
								if (jsonObject.has("result") && jsonObject.get("result").getAsString().equalsIgnoreCase("success")) lastBuild = b;
							}
						} else break;
					} catch (IOException ex) {
						break;
					}
				}
				
				if (lastBuild != build) {
					final int b = lastBuild;
					
					boolean loaded = BungeeCord.isLoaded();
					BungeeCord.unload(() -> {
						System.out.println("Updating BungeeCord...");
						try {
							HttpURLConnection connection = ((HttpURLConnection) new URL(bungeeUrl.replaceAll("%build%", ""+b)).openConnection());
							connection.setRequestProperty("User-Agent", userAgent);
							if (connection.getResponseCode() == 200) 
								IOUtils.copy(connection.getInputStream(), new FileOutputStream("bungeecord.jar"));
							System.out.println("BungeeCord updated");
						} catch (IOException ex) {
							System.out.println("BungeeCord update failed");
							ex.printStackTrace();
						}
						if (loaded) BungeeCord.load();
					});
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			updatingBungee = false;
		}
	}
	
}