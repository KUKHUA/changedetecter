/*
 * ChangeDetector -  detects modifications, deletions, or creations of files and folders.
 * Copyright (C) 2025 KUKHUA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package watcher;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import http.ClientStore;  
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import misc.GetEventJSON;
import java.nio.file.Path;
import misc.FileFilter;
import misc.Config;
import ANSI.Print;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * The {@code WebhookSend} class implements the {@link IWatchCallback} interface and is responsible for
 * sending JSON-formatted event data to a list of configured webhook URLs.
 * <p>
 * When an event occurs, the {@code onEvent} method is triggered, which:
 * <ul>
 *   <li>Filters the event using {@link FileFilter#isAllowed(Path)}.</li>
 *   <li>Builds a JSON object representing the event using {@link GetEventJSON#run(String, String, Path)}.</li>
 *   <li>Sends the JSON payload to each webhook URL using an HTTP POST request.</li>
 * </ul>
 * <p>
 * @see IWatchCallback
 * @see SSESend
 * @see GetEventJSON
 * @see FileFilter
 */
public final class WebhookSend implements IWatchCallback {
    // Cache webhook URLs to avoid repeated parsing
    private String[] cachedWebHookList = null;

    public WebhookSend() {
        // Initialize webhook list cache
        Config config = Config.instance();
        String inputArrayString = config.getDefault("sources.webhook.urls","null,null");
        cachedWebHookList = Arrays.stream(inputArrayString.split(","))
            .map(String::trim)
            .filter(string -> !string.equalsIgnoreCase("null"))
            .toArray(String[]::new);
    }

    public void onEvent(String changeType, String fullPath, Path path) {
        if(!FileFilter.isAllowed(path)) return;

        JSONObject object = GetEventJSON.run(changeType, fullPath, path);

        for (String webhook : cachedWebHookList) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(webhook);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = object.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    ANSI.Print.setFront(112);
                    System.out.println("Webhook POST successful to: " + webhook + " (HTTP " + responseCode + ")");
                    ANSI.Print.unsetFront();
                } else {
                    ANSI.Print.setFront(196);
                    System.out.println("Webhook POST failed to: " + webhook + " (HTTP " + responseCode + ")");
                    ANSI.Print.unsetFront();
                }

            } catch (Exception e) {
                ANSI.Print.setFront(196);
                System.out.println("Unable to send data to webhook " + webhook + ". Error: " + e);
                e.printStackTrace();
                ANSI.Print.unsetFront();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                } 
            }
        }
    }
}