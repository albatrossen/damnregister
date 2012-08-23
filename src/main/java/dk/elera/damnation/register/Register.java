/*
 *  Copyright 2012 Jes Andersen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package dk.elera.damnation.register;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Register extends JavaPlugin {

	Pattern email_pattern = Pattern.compile("^.+@.+\\..+");
	private URL url;
	private String body;
	private int longest_response = 0;
	private HashMap<String, String> answers = new HashMap<String, String>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		try {
			url = new URL(getConfig().getString("url"));
		} catch (MalformedURLException e) {
			getLogger().severe("The url specified in the config is not valid");
		}
		body = getConfig().getString("body");

		List<Map<?, ?>> answerslist = getConfig().getMapList("answers");
		for (Map<?, ?> x : answerslist) {
			String response = (String) x.get("response");
			String answer = (String) x.get("answer");
			if (response != null && answer != null && !response.trim().isEmpty()) {
				answers.put(response, answer);
				longest_response = Math.max(longest_response, response.length());
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			if (args.length != 1 || !email_pattern.matcher(args[0]).matches()) {
				sender.sendMessage(ChatColor.RED + "You must supply an valid email for the registration");
			} else {
				register((Player) sender, args[0]);
			}
		} else {
			sender.sendMessage(ChatColor.RED + "this command can only be run by a player");
		}
		return true;
	}

	private void register(final Player player, final String email) {
		final String name = player.getName();
		getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {

			public void run() {
				String response = getUrl(String.format(body, name, email), longest_response);
				if (answers.containsKey(response)) {
					sendMessage(player, answers.get(response));
				} else {
					sendMessage(player, ChatColor.RED + "The is currently an internal failure - please contact an admin");
					getLogger().severe("/register url made unknown response: " + response);
				}
			}
		});
	}

	/**
	 * This method gets the contents of a POST url, and returns up to max_length
	 * chars of the response
	 * 
	 * @param data
	 *            POST data to transmit to the url
	 * @param max_length
	 *            Max lenght of returned string
	 * @return
	 */
	protected String getUrl(String data, int max_length) {
		URLConnection connection;
		getLogger().info("max_length is " + max_length);
		char[] buffer = new char[max_length];
		try {
			connection = url.openConnection();
			connection.setDoOutput(true);

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(data);
			writer.flush();

			InputStreamReader reader = new InputStreamReader(connection.getInputStream());
			reader.read(buffer, 0, max_length);
			reader.close();
			writer.close();
		} catch (IOException e) {
			getLogger().severe("Failed to call /register url: " + e.getMessage());
			return null;
		}
		return String.valueOf(buffer);
	}

	private void sendMessage(final Player player, final String message) {
		getServer().getScheduler().scheduleSyncDelayedTask(Register.this, new Runnable() {

			public void run() {
				if (player.isOnline()) {
					player.sendMessage(message);
				}
			}
		});
	}

}
