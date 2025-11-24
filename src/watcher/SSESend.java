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
import misc.GetEventJSON;
import java.nio.file.Path;
import misc.FileFilter;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * The {@code SSESend} class implements the {@link IWatchCallback} interface and is responsible for
 * sending JSON-formatted event data to connected clients via Server-Sent Events (SSE).
 * <p>
 * When an event occurs, the {@code onEvent} method is triggered, which:
 * <ul>
 *   <li>Filters the event using {@link FileFilter#isAllowed(Path)}.</li>
 *   <li>Builds a JSON object representing the event using {@link GetEventJSON#run(String, String, Path)}.</li>
 *   <li>Sends the JSON payload to each connected client via their SSE stream.</li>
 * </ul>
 * <p>
 * @see IWatchCallback
 * @see WebhookSend
 * @see GetEventJSON
 * @see FileFilter
 */

public final class SSESend implements IWatchCallback {
    public void onEvent(String changeType, String fullPath, Path path) {
        if(!FileFilter.isAllowed(path)) return;

        JSONObject object = GetEventJSON.run(changeType, fullPath, path);

        // Use iterator to safely remove clients during iteration
        java.util.Iterator<HttpExchange> iterator = ClientStore.get().clients().iterator();
        while (iterator.hasNext()) {
            HttpExchange client = iterator.next();
            try {
                client.getResponseBody().write(("data:" + object.toString() + "\n\n").getBytes());

                client.getResponseBody().flush();

            } catch (Exception e) {
                ANSI.Print.setFront(196);
                System.out.println("One of the clients has disconnected, removing it from the list.");
                ANSI.Print.unsetFront();
                iterator.remove();
            }
        }
    }
}