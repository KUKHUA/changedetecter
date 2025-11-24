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

package misc; 

import java.nio.file.*; 
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * The {@code FileFilter} class provides utility methods for filtering and classifying file paths.
 * It helps determine if a file or directory should be considered "allowed" based on certain criteria
 * (e.g., not hidden, not a partial file) and to guess the type of a file system object
 * (e.g., "folder", "file").
 */
public class FileFilter {

    // Cache for blacklisted files and folders to avoid repeated parsing
    private static Set<String> blacklistedFilesCache = null;
    private static Set<String> blacklistedFoldersCache = null;

    /**
     * Checks if a given {@link Path} is allowed based on a set of predefined rules.
     * A path is generally disallowed if:
     * <ul>
     *     <li>It resides within a "hidden" parent directory structure (e.g., contains '..' in its parent path).</li>
     *     <li>Its filename ends with ".part", indicating an incomplete file.</li>
     *     <li>It exists and is marked as a hidden file by the operating system.</li>
     *     <li>It does not exist but its filename starts with a dot ('.'),  indicating a hidden file.</li>
     * </ul>
     * If an exception occurs during the checks, an error message is printed to the console,
     * and the method returns {@code false}.
     *
     * @param path The {@link Path} to check.
     * @return {@code true} if the path is allowed according to the rules, {@code false} otherwise.
     */
    public static boolean isAllowed(Path path) {
        try {
            if(isBlacklisted(path)) return false;

            // Optimize: count dots without stream operations
            String parentPath = path.getParent().toString();
            int dotCount = 0;
            for (int i = 0; i < parentPath.length(); i++) {
                if (parentPath.charAt(i) == '.') dotCount++;
            }
            if (dotCount == 2) return false; // hide files with exactly two dots in parent path

            if (path.getFileName().toString().endsWith(".part")) return false; // hide parts of a file

             if (Files.exists(path)) {
                path = path.toRealPath(); 

                if(Files.isHidden(path)) return false; //hide hidden files that exist
            } else {
                return !path.getFileName().toString().startsWith("."); // hide hidden files that dont exist, and show files that dont exist yet are not hidden
            }

            return true;

        } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
    e.printStackTrace();  
            return false;
        }
    }

    /**
     * Determines the type of the file system object represented by the given {@link Path}.
     * It first checks if the path points to a directory or a regular file.
     * If neither, it attempts to guess the type based on the filename.
     *
     * @param path The {@link Path} to classify.
     * @return A string representing the object type:
     *         "folder" if it's a directory,
     *         "file" if it's a regular file,
     *         or a guessed type ("maybe_file", "maybe_folder", "magical_thing") otherwise.
     */
    public static String objectType(Path path){
        return Files.isDirectory(path) ? "folder" : Files.isRegularFile(path) ? "file" : guess(path);
    }

    /**
     * Guesses the type of a file system object based on its filename.
     * This method is used as a fallback when {@link Files#isDirectory(Path, java.nio.file.LinkOption...)}
     * and {@link Files#isRegularFile(Path, java.nio.file.LinkOption...)} do not provide a definitive answer.
     * <ul>
     *     <li>If the filename is empty, it's considered a "magical_thing".</li>
     *     <li>If the filename contains a dot ('.'), it's guessed to be a "maybe_file".</li>
     *     <li>If the filename does not contain a dot, it's guessed to be a "maybe_folder".</li>
     *
     * @param path The {@link Path} whose type is to be guessed.
     * @return A string representing the guessed type: "maybe_file", "maybe_folder", or "magical_thing".
     */
    private static String guess(Path path) {
        String name = path.getFileName().toString();

        if (name.contains(".")) {
            return "maybe_file";
        } else if (!name.contains(".")) {
            return "maybe_folder";
        }

        return "magical_thing";
    }

    /**
     * Initializes the blacklist cache by parsing configuration values.
     * This method should be called once during application startup.
     */
    public static void initializeBlacklists() {
        Config config = Config.instance();

        String blacklistedFileString = config.getDefault("blacklisted.files","null,null");
        blacklistedFilesCache = new HashSet<>(Arrays.asList(
            Arrays.stream(blacklistedFileString.split(","))
                .map(String::trim)
                .filter(string -> !string.equalsIgnoreCase("null"))
                .toArray(String[]::new)
        ));

        String blacklistedFolderString = config.getDefault("blacklisted.folders","null,null");
        blacklistedFoldersCache = new HashSet<>(Arrays.asList(
            Arrays.stream(blacklistedFolderString.split(","))
                .map(String::trim)
                .filter(string -> !string.equalsIgnoreCase("null"))
                .toArray(String[]::new)
        ));
    }

    public static boolean isBlacklisted(Path path){
        // Initialize cache on first access if not already initialized
        if (blacklistedFilesCache == null || blacklistedFoldersCache == null) {
            initializeBlacklists();
        }

        boolean hasBlacklistedFolder = blacklistedFoldersCache.contains(path.getParent().toString());
        boolean hasBlackListedFile = blacklistedFilesCache.contains(path.getFileName().toString());

        return hasBlackListedFile || hasBlacklistedFolder;
    }

}
