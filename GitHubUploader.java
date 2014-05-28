package at.electrobabe.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * util class for uploading binaries (APK) to github
 *
 * @author electrobabe
 * @version version 0.2
 */
public class GitHubUploader {

    private static final Logger logger = Logger.getLogger("GitHubUploader");

    private static final String GITHUB_API_URL = "https://api.github.com/repos/:user/:repo/";

    private static final String GITHUB_API_RELEASES_URL = GITHUB_API_URL + "releases?per_page=1";
    // OAUTH_TOKEN of github user
    private static final String AUTH_TOKEN = ":token";

    private static final String UPLOAD_URL_PREFIX = "\"upload_url\":\"";
    private static final String UPLOAD_URL_SUFFIX = "{?name}\"";

    private static final String TAG_PREFIX = "\"tag_name\":\"";
    private static final String ASSET_NAME_PREFIX = ",\"name\":\"";
    private static final String ASSET_ID_PREFIX = "\"id\":";

    private static final String ASSET_URL = GITHUB_API_URL + "releases/assets/";

    private static final int BUFFER_SIZE = 4096;

    /**
     * @param args [0] filename required
     */
    public static void main(String[] args) {
        logger.log(Level.INFO, "Start GitHubUploader");

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

            logger.log(Level.INFO, "POST ok with " + retCode + " " + url + "?name=" + file.getName());

            if (retCode == 201) {
                logger.log(Level.INFO, "Upload of " + file.getName() + " to " + url + " was successful");
            } else {
                logger.log(Level.INFO, "Error uploading " + file.getName() + " to " + url);
                System.exit(-retCode);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in uploadFile. Maybe file exists at GitHub?");

        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * write file to stream
     *
     * @param conn     HttpURLConnection
     * @param fileFile File
     * @throws IOException
     */
    private static void writeData(HttpURLConnection conn, File fileFile) throws IOException {

        final String CRLF = "\r\n";
        final String BOUNDARY = "*****";

        OutputStream output = conn.getOutputStream();

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
        // set some headers with writer
        InputStream file = new ByteArrayInputStream(getFileAsByteArray(fileFile));
        System.out.println("Size: " + file.available());

        byte[] buffer = new byte[BUFFER_SIZE];
        int length;
        while ((length = file.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }
        output.flush();
        writer.append(CRLF).flush();
        writer.append("--").append(BOUNDARY).append("--").append(CRLF).flush();

        output.close();
    }

    static byte[] getFileAsByteArray(File file) throws IOException {

        FileInputStream fileStream = new FileInputStream(file);

        // Instantiate array
        byte[] arr = new byte[(int) file.length()];

        /// read All bytes of File stream
        int i = fileStream.read(arr, 0, arr.length);

        return arr;
    }

    /**
     * @return String e.g. https://uploads.github.com/repos/:user/:repo/releases/123/assets
     */
    static String getUploadUrl(String fileName) {
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
            logger.log(Level.INFO, "tag: " + tag);

            deleteOldAsset(ret.toString(), fileName);

            return ret.substring(ret.indexOf(UPLOAD_URL_PREFIX) + UPLOAD_URL_PREFIX.length(), ret.indexOf(UPLOAD_URL_SUFFIX));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error in getUploadUrl");

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
            logger.log(Level.INFO, "Asset " + new File(fileName).getName() + " already exists, delete old asset");

            // "assets":[{"url":"https://api.github.com/repos/:user/:repo/releases/assets/123","id":123,
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
                logger.log(Level.INFO, "DELETE ok with " + retCode + " " + ASSET_URL + assetId);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "error in getUploadUrl");

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
