import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * util class for uploading binaries (APK) to github
 *
 * @author electrobabe
 */
public class GitHubUploader {
    private final static String GITHUB_API_URL = "https://api.github.com/repos/:user/:repo/";

    private final static String GITHUB_API_RELEASES_URL = GITHUB_API_URL + "releases?per_page=1";
    // OAUTH_TOKEN for github
    private static final String AUTH_TOKEN = "mysecret";

    private static final String UPLOAD_URL_PREFIX = "\"upload_url\":\"";
    private static final String UPLOAD_URL_SUFFIX = "{?name}\"";

    private static final String TAG_PREFIX = "\"tag_name\":\"";
    private static final String ASSET_NAME_PREFIX = ",\"name\":\"";
    private static final String ASSET_ID_PREFIX = "\"id\":";

    private static final String ASSET_URL = GITHUB_API_URL + "releases/assets/";

    /**
     * @param args [0] filename required
     */
    public static void main(String[] args) {
        System.out.println("Start GitHubUploader");

        String fileName = args[0];
        String url = getUploadUrl(fileName);

        uploadFile(url, fileName);
    }

    /**
     * @param url String
     */
    private static void uploadFile(String url, String fileName) {
        HttpURLConnection conn = null;
        File file = new File(fileName);

        try {
            //Create connection
            URL server = new URL(url + "?name=" + file.getName());
            conn = (HttpURLConnection) server.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "token " + AUTH_TOKEN);
            conn.setRequestProperty("Content-Type", "application/vdn.android.package-archive");

            writeData(conn, file);

            conn.connect();

            int retCode = conn.getResponseCode();

            System.out.println("POST ok with " + retCode + " " + url + "?name=" + file.getName());

            if (retCode == 201) {
                System.out.println("Upload of " + file.getName() + " to " + url + " was successful");
            } else {
                System.out.println("Error uploading " + file.getName() + " to " + url);
                System.exit(-retCode);
            }


        } catch (Exception e) {
            System.out.println("Error in uploadFile. Maybe file exists at GitHub?");
            e.printStackTrace();

        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * write file to stream
     *
     * @param conn HttpURLConnection
     * @param file File
     * @throws IOException
     */
    private static void writeData(HttpURLConnection conn, File file) throws IOException {

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer;
        writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

        writer.write(getFileAsString(file));
        writer.flush();
        writer.close();
        os.close();
    }

    private static String getFileAsString(File file) throws IOException {
        StringBuilder buffer = new StringBuilder();
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String str;
        while ((str = br.readLine()) != null) {
            buffer.append(str);
        }
        return buffer.toString();
    }

    /**
     * @return String e.g. https://uploads.github.com/repos/keyosk-tablets/keyosk-android/releases/294726/assets
     */
    private static String getUploadUrl(String fileName) {
        HttpURLConnection conn = null;

        try {
            //Create connection
            URL server = new URL(GITHUB_API_RELEASES_URL);
            conn = (HttpURLConnection) server.openConnection();

            conn.setRequestMethod("GET");
            // conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "token " + AUTH_TOKEN);

            conn.connect();
            StringBuffer ret = getResponse(conn);

            String tag = ret.substring(ret.indexOf(TAG_PREFIX) + TAG_PREFIX.length());
            tag = tag.substring(0, tag.indexOf("\""));
            System.out.println("tag: " + tag);

            deleteOldAsset(ret.toString(), fileName);

            return ret.substring(ret.indexOf(UPLOAD_URL_PREFIX) + UPLOAD_URL_PREFIX.length(), ret.indexOf(UPLOAD_URL_SUFFIX));

        } catch (Exception e) {
            System.out.println("error in getUploadUrl");
            e.printStackTrace();

        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private static void deleteOldAsset(String s, String fileName) {
        String searchValue = ASSET_NAME_PREFIX + new File(fileName).getName() + "\"";

        if (s.contains(searchValue)) {
            System.out.println("Asset " + new File(fileName).getName() + " already exists, delete old asset");

            // "assets":[{"url":"https://api.github.com/repos/keyosk-tablets/keyosk-android/releases/assets/125222","id":125222,
            String assetId = s.substring(s.indexOf("\"assets\""), s.indexOf(searchValue));
            assetId = assetId.substring(assetId.lastIndexOf(ASSET_ID_PREFIX) + ASSET_ID_PREFIX.length());

            HttpURLConnection conn = null;

            try {
                //Create connection
                URL server = new URL(ASSET_URL + assetId);
                conn = (HttpURLConnection) server.openConnection();

                conn.setRequestMethod("DELETE");
                // conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "token " + AUTH_TOKEN);

                conn.connect();

                int retCode = conn.getResponseCode();
                System.out.println("DELETE ok with " + retCode + " " + ASSET_URL + assetId);

            } catch (Exception e) {
                System.out.println("error in getUploadUrl");
                e.printStackTrace();

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private static StringBuffer getResponse(HttpURLConnection conn) throws IOException {
        InputStreamReader in = new InputStreamReader((InputStream) conn.getContent());
        BufferedReader buff = new BufferedReader(in);
        String line;
        StringBuffer ret = new StringBuffer();
        do {
            line = buff.readLine();
            if (line != null) {
                ret.append(line).append("\n");
            }
        } while (line != null);
        return ret;
    }
}
