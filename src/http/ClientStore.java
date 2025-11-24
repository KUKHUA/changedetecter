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

package http;

import com.sun.net.httpserver.HttpExchange;
import java.util.concurrent.CopyOnWriteArrayList;

/** 
 * The {@code ClientStore} class is a singleton that manages a collection of active HTTP client connections.
 * It allows adding new clients and retrieving the list of currently connected clients.
 * Uses CopyOnWriteArrayList for thread-safe operations.
 */
public final class ClientStore {

    private CopyOnWriteArrayList<HttpExchange> clients = new CopyOnWriteArrayList<HttpExchange>();
    private static ClientStore instance = null;

    private ClientStore(){}

    public void addClient(HttpExchange t) {
        clients.add(t);
    }

    public CopyOnWriteArrayList<HttpExchange> clients() {
        return clients;
    }

    public static ClientStore get(){
        if(instance == null) instance = new ClientStore();
        return instance;
    }
}
