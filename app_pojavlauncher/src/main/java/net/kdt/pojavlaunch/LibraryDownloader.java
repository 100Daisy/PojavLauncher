package net.kdt.pojavlaunch;

import android.os.AsyncTask;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.utils.DownloadUtils;

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
            DownloadTask dltask = new DownloadTask();
            dltask.execute(url, directory + "/" + artifactId + "-" + version + ".jar");
            }
        }
    private static class DownloadTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String fileUrl = params[0];
            String filePath = params[1];
            String[] nameComponents = filePath.split("/");
            try {
                DownloadUtils.downloadFileMonitored(fileUrl, filePath, new byte[1024], (curr, max) -> {
                    ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK,(curr * 100)/max, String.format("Downloading Library: %s", nameComponents[nameComponents.length-1]));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    }