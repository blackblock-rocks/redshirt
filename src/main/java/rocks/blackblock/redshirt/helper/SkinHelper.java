package rocks.blackblock.redshirt.helper;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.Nullable;
import rocks.blackblock.bib.collection.LRUCache;
import rocks.blackblock.bib.util.BibJson;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.redshirt.Redshirt;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Help for working with Skins.
 *
 * @author   Samo_Lego
 * @author   Jelle De Loecker   <jelle@elevenways.be>
 * @since    0.1.0
 */
public class SkinHelper {

    private static final BibLog.Categorised LOGGER = BibLog.getCategorised("redshirt", "skinhelper");

    private static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();
    private static final LRUCache<String, Property> SKIN_CACHE = new LRUCache<>(25);
    private static String rincemaft_endpoint = null;

    /**
     * Log an error
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    private static void errorLog(String message) {
        Redshirt.LOGGER.error(message);
    }

    /**
     * Get skin data
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     *
     * @param   skin_source   The target to get the skin data from
     * @param   use_slim      Whether slim format should be used
     *
     * @callback  Property containing skin value and signature if successful, otherwise null.
     */
    public static void getSkin(String skin_source, Boolean use_slim, SkinPropertyResult callback) {

        String key = skin_source + use_slim;

        if (SKIN_CACHE.containsKey(key)) {
            callback.onResult(SKIN_CACHE.get(key));
            return;
        }

        if (LOGGER.isEnabled()) {
            LOGGER.log("Getting skin with source argument '" + skin_source + "'");
            LOGGER.log("  - Use slim:", use_slim);
            Thread.dumpStack();
        }

        if (rincemaft_endpoint == null) {
            Redshirt.CONFIG.afterLoad(() -> {
                rincemaft_endpoint = Redshirt.CONFIG.getRincemaftEndpoint();

                // If there still is no endpoint, the configuration has none
                if (rincemaft_endpoint == null) {
                    callback.onResult(null);
                    return;
                }

                getSkin(skin_source, use_slim, callback);
            });

            return;
        }

        THREADPOOL.execute(() -> {

            final Property result;

            boolean is_slim = use_slim;

            if (skin_source.startsWith("http:") || skin_source.startsWith("https:")) {
                result = fetchSkinByUrl(skin_source, is_slim);
            } else if (skin_source.contains("/")) {
                result = fetchSkinFromFile(skin_source, is_slim);
            } else {
                result = fetchSkinByName(skin_source);
            }

            // Only cache successful skin requests
            if (result == null) {
                BibLog.error("Failed to get skin from '" + skin_source + "'");
            } else {
                SKIN_CACHE.put(key, result);
            }

            Redshirt.SERVER.execute(() -> callback.onResult(result));
        });
    }

    /**
     * Callback interface
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    @FunctionalInterface
    public interface SkinPropertyResult {
        void onResult(Property result);
    }

    /**
     * Gets skin data from file.
     *
     * @param skinFilePath file path
     * @param useSlim whether slim format should be used
     * @return property containing skin value and signature if successful, otherwise null.
     */
    private static Property fetchSkinFromFile(String skinFilePath, boolean useSlim) {
        File skinFile = new File(skinFilePath);
        try (FileInputStream input = new FileInputStream(skinFile)) {
            if(input.read() == 137) {
                try {
                    String reply = urlRequest(new URL(rincemaft_endpoint + "/api/v1/skin/sign"), false, skinFile);
                    return getSkinFromReply(reply);
                } catch (IOException e) {
                    // Error uploading
                    errorLog(e.getMessage());
                }
            }
        } catch (IOException e) {
            // Not an image
            errorLog(e.getMessage());
        }
        return null;
    }

    /**
     * Sets skin setting from the provided URL.
     *
     * @param skinUrl string url of the skin
     * @return property containing skin value and signature if successful, otherwise null.
     */
    @Nullable
    private static Property fetchSkinByUrl(String skinUrl, boolean useSlim) {
        try {
            URL url = new URL(String.format(rincemaft_endpoint + "/api/v1/skin/sign?url=%s", skinUrl));
            String reply = urlRequest(url, false, null);
            return getSkinFromReply(reply);
        } catch (IOException e) {
            errorLog(e.getMessage());
        }
        return null;
    }

    /**
     * Sets skin by playername.
     *
     * @param playername name of the player who has the skin wanted
     * @return property containing skin value and signature if successful, otherwise null.
     */
    @Nullable
    private static Property fetchSkinByName(String playername) {

        if (playername == null || playername.isBlank()) {
            return null;
        }

        try {
            URL url = new URL(String.format(rincemaft_endpoint + "/api/v1/skin/sign?username=%s", playername));
            String reply = urlRequest(url, false, null);
            return getSkinFromReply(reply);
        } catch (IOException e) {
            errorLog(e.getMessage());
        }
        return null;
    }

    /**
     * Sets skin from reply that was got from API.
     * Used internally only.
     *
     * @param reply API reply
     * @return property containing skin value and signature if successful, otherwise null.
     */
    @Nullable
    protected static Property getSkinFromReply(String reply) {

        if (reply == null || reply.isEmpty()) {
            return null;
        }

        JsonObject obj = BibJson.parse(reply);

        if (obj.has("error") && obj.get("error").getAsBoolean()) {
            return null;
        }

        JsonObject texture = obj.getAsJsonObject("texture");

        if (texture == null) {
            return null;
        }

        JsonObject skin = texture.getAsJsonObject("skin");

        if (skin == null) {
            return null;
        }

        String value = skin.get("value").getAsString();
        String signature = skin.get("signature").getAsString();

        return new Property("textures", value, signature);
    }

    /**
     * Gets reply from a skin website.
     * Used internally only.
     *
     * @param url url of the website
     * @param useGetMethod whether to use GET method instead of POST
     * @param image image to upload, otherwise null
     * @return reply from website as string
     * @throws IOException IOException is thrown when connection fails for some reason.
     */
    private static String urlRequest(URL url, boolean useGetMethod, @Nullable File image) throws IOException {
        URLConnection connection = url.openConnection();

        String reply = null;

        if(connection instanceof HttpsURLConnection httpsConnection) {
            httpsConnection.setUseCaches(false);
            httpsConnection.setDoOutput(true);
            httpsConnection.setDoInput(true);
            httpsConnection.setRequestMethod(useGetMethod ? "GET" : "POST");

            if (image != null) {
                String boundary = UUID.randomUUID().toString();
                httpsConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                httpsConnection.setRequestProperty("User-Agent", "User-Agent");

                OutputStream outputStream = httpsConnection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

                final String LINE = "\r\n";
                writer.append("--").append(boundary).append(LINE);
                writer.append("Content-Disposition: form-data; name=\"file\"").append(LINE);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE);
                writer.append(LINE);
                writer.append(image.getName()).append(LINE);
                writer.flush();

                writer.append("--").append(boundary).append(LINE);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(image.getName()).append("\"").append(LINE);
                writer.append("Content-Type: image/png").append(LINE);
                writer.append("Content-Transfer-Encoding: binary").append(LINE);
                writer.append(LINE);
                writer.flush();

                byte[] fileBytes =  Files.readAllBytes(image.toPath());
                outputStream.write(fileBytes,  0, fileBytes.length);

                outputStream.flush();
                writer.append(LINE);
                writer.flush();

                writer.append("--").append(boundary).append("--").append(LINE);
                writer.close();
            }

            if (LOGGER.isEnabled()) {
                LOGGER.log("Requesting", url);
                LOGGER.log("  - Method:", httpsConnection.getRequestMethod());
                LOGGER.log("  - Content type:", httpsConnection.getContentType());
                LOGGER.log("  - Image:", image);
                LOGGER.log(" -- Response:", httpsConnection.getResponseCode());
            }

            if (httpsConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reply = getContent(connection);
            }

            httpsConnection.disconnect();
        }
        else {
            reply = getContent(connection);
        }

        return reply;
    }

    /**
     * Reads response from API.
     * Used just to avoid duplicate code.
     *
     * @param connection connection where to take output stream from
     * @return API reply as String
     * @throws IOException exception when something went wrong
     */
    private static String getContent(URLConnection connection) throws IOException {
        try (
                InputStream is = connection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                Scanner scanner = new Scanner(isr)
        ) {
            StringBuilder reply = new StringBuilder();
            while(scanner.hasNextLine()) {
                String line = scanner.next();
                if(line.trim().isEmpty())
                    continue;
                reply.append(line);
            }

            return reply.toString();
        }
    }

}
