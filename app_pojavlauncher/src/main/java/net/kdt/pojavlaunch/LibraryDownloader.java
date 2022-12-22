package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.ModpackInstaller.downloadFiles;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

public class LibraryDownloader {
    public static void downloadLibraries(String jsonString) throws Exception {
        JSONObject json = new JSONObject(jsonString);
        JSONArray libraries = json.getJSONArray("libraries");

        for (int i = 0; i < libraries.length(); i++) {
            JSONObject library = libraries.getJSONObject(i);
            String name = library.getString("name");
            String url = library.getString("url");

            // Split the library name into its components
            String[] nameComponents = name.split(":");
            String groupId = nameComponents[0];
            String artifactId = nameComponents[1];
            String version = nameComponents[2];

            String[] splitGroupId = groupId.split("\\.");
            Path path = Paths.get("");
            for (String s : splitGroupId) {
                path = path.resolve(s);
            }
            // Create the folder structure for the library
            Path directory = Paths.get(Tools.DIR_GAME_HOME, ".minecraft", "libraries", String.valueOf(path), artifactId, version);
            Log.d("TEST", String.valueOf(directory));
            Files.createDirectories(directory);
            new DownloadTask().execute(url, directory + "/" + artifactId + "-" + version + ".jar");
            }
        }
    private static class DownloadTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String fileUrl = params[0];
            String filePath = params[1];
            try {
                // Download the file and save it to the "mods" folder
                URL url = new URL(fileUrl);
                BufferedInputStream in = new BufferedInputStream(url.openStream());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filePath));
                byte[] buffer = new byte[1024];
                int numRead;
                while ((numRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, numRead);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    }