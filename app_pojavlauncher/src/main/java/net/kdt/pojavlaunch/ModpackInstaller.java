package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.MainActivity.INTENT_MINECRAFT_VERSION;
import static net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.launcherProfilesFile;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftLauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class ModpackInstaller extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final File modFile = (File) getIntent().getExtras().getSerializable("modFile");
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(modFile))) {
            // Iterate through the entries in the zip file
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                // If the entry is a JSON file
                if (entry.getName().endsWith(".json")) {
                    // Read the JSON file into a string
                    StringBuilder jsonStringBuilder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonStringBuilder.append(line);
                    }
                    // Parse the JSON string into a JSON object
                    JSONObject json = new JSONObject(jsonStringBuilder.toString());
                    String modpackName = ".minecraft";
                    // Do something with the JSON object
                    JSONObject dependencies =  json.getJSONObject("dependencies");
                    Log.d("Modpack", String.valueOf(dependencies));
                    if (dependencies.has("fabric-loader")) {
                        Object value = dependencies.get("fabric-loader");
                        String value2 = String.valueOf(dependencies.get("minecraft"));
                        JMinecraftVersionList.Version mcVersion = AsyncMinecraftDownloader.getListedVersion(value2);
                        new AsyncMinecraftDownloader(this, mcVersion, value2, () -> runOnUiThread(() -> {
                            try {
                                MinecraftProfile profile = MinecraftProfile.createTemplate();
                                profile.name = "Modden";
                                addProfile(profile);
                                Intent mainIntent = new Intent(getBaseContext(), MainActivity.class);
                                mainIntent.putExtra(INTENT_MINECRAFT_VERSION, value2);
                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(mainIntent);
                                finish();
                                android.os.Process.killProcess(android.os.Process.myPid()); //You should kill yourself, NOW!
                            } catch (Throwable e) {
                                Tools.showError(getBaseContext(), e);
                            }
                        }));
                        Log.d("xdDXdsadasdasds", String.valueOf(value));
                        Path directory = Paths.get(Tools.DIR_GAME_HOME, ".minecraft", "versions", String.format("fabric-loader-%s-%s", value, value2));
                        Files.createDirectories(directory);
                        new DownloadTask().execute(String.format("https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json", value2,value), String.format("%s/%s/versions/fabric-loader-%s-%s/fabric-loader-%s-%s.json",Tools.DIR_GAME_HOME, ".minecraft", value, value2, value, value2));
                        String file = String.format("%s/%s/versions/fabric-loader-%s-%s/fabric-loader-%s-%s.json",Tools.DIR_GAME_HOME, ".minecraft", value, value2, value, value2);
                        String json2 = new String(Files.readAllBytes(Paths.get(file)));
                        new File(String.format("%s/%s/versions/fabric-loader-%s-%s/fabric-loader-%s-%s.jar",Tools.DIR_GAME_HOME, ".minecraft", value, value2, value, value2)).createNewFile();
                        LibraryDownloader.downloadLibraries(json2);
                    }
                    // do something with the key and value
                    downloadFiles(json, modpackName);
                }
                // Get the next entry in the zip file
                entry = zipInputStream.getNextEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addProfile(MinecraftProfile profile) {
        try {
            // Update the mainProfileJson object
            MinecraftLauncherProfiles mainProfileJson = LauncherProfiles.update();
            mainProfileJson.profiles.put(profile.name, profile);
            // Write the updated mainProfileJson object to the launcher_profiles.json file
            Tools.write(launcherProfilesFile.getAbsolutePath(), mainProfileJson.toJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void downloadFiles(JSONObject json, String instanceDir) throws IOException, JSONException {
        // Get the "files" array from the JSON object
        JSONArray files = json.getJSONArray("files");

        // Create the "mods" folder if it doesn't exist
        File modsFolder = new File(String.format("%s/%s/mods", Tools.DIR_GAME_HOME, instanceDir));
        if (!modsFolder.exists()) {
            modsFolder.mkdir();
        }

        // Iterate through the "files" array and download each file using AsyncTask
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String filePath = String.format("%s/%s/%s",Tools.DIR_GAME_HOME, instanceDir, file.getString("path"));
            File file2 = new File(filePath);
            file2.createNewFile();
            JSONArray downloads = file.getJSONArray("downloads");
            String fileUrl = downloads.getString(0);

            new DownloadTask().execute(fileUrl, filePath);
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
