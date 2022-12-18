import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class InstallModpack extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_DEFAULT)) {
      Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
      ContentResolver contentResolver = getContentResolver();
      InputStream inputStream = contentResolver.openInputStream(fileUri);
      // Call the necessary methods of the InstallModloader class
      // Open the zip file
      try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(inputStream))) {
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
            // Do something with the JSON object
            FileDownloader.downloadFiles(json);
          }
          // Get the next entry in the zip file
          entry = zipInputStream.getNextEntry();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

class FileDownloader {
  public static void downloadFiles(JSONObject json) throws IOException {
    // Get the "files" array from the JSON object
    //
    JSONArray files = json.getJSONArray("files");

    // Create the "mods" folder if it doesn't exist
    File modsFolder = new File("mods");
    if (!modsFolder.exists()) {
      modsFolder.mkdir();
    }

    Thread thread = new Thread(() -> {
      // Iterate through the "files" array and download each file
      for (int i = 0; i < files.length(); i++) {
        JSONObject file = files.getJSONObject(i);
        String filePath = file.getString("path");
        JSONArray downloads = file.getJSONArray("downloads");
        String fileUrl = downloads.getString(0);

        // Download the file and save it to the "mods" folder
        URL url = new URL(fileUrl);
        FileOutputStream outputStream;
        outputStream = openFileOutput(".minecraft/" + filePath, Context.MODE_PRIVATE);
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
          outputStream.write(buffer, 0, numRead);
        }
        in.close();
        outputStream.close();
      }
    });
  }
}