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

package commands;
import Command.IHandler;
import Command.Command;
import misc.Config;
import misc.FileFilter;
import http.http;
import http.SSEConnect;
import watcher.SSESend;
import watcher.WebhookSend;
import watcher.FolderWatcher; 

/**
 * The {@code StartCommand} class implements the IHandler interface to handle the start command for the application.
 * It initializes and starts the FolderWatcher, sets up Server-Sent Events (SSE) if enabled, and configures webhook sending.
 */
public class StartCommand implements IHandler {
    @Override
    public void handleCommand(Command command) throws Exception {
        Config config = Config.instance();
        
        // Initialize blacklist cache once at startup for better performance
        FileFilter.initializeBlacklists();
        
        boolean sseEnabled = config.getDefault("sources.sse.enabled", "false").equals("true");
         
        if(sseEnabled){
            String port = config.getDefault("sources.sse.port", "1234"); 
            http server = new http(Integer.parseInt(port));
            server.start();
            server.addHandler("/", new SSEConnect());
            System.out.println("Started SSE Server on port " + port + ".");
        }

        boolean webhookEnabled = config.getDefault("sources.webhook.enabled","false").equals("true");


        FolderWatcher watcher = new FolderWatcher(".");
        if(webhookEnabled) {
            System.out.println("Started webhook sender.");
            watcher.addCallBack(new WebhookSend());
        }
        
        if(sseEnabled) watcher.addCallBack(new SSESend());
        watcher.startWatching();
    }

    @Override
    public String getHelpInfo(){
        return "Actually starts looking for changes and broadcast them.";
    }
}