package com.chadvidcraft.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class CloudApi {
    private static final String PREFS = "chadvidcraft_cloud";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "display_name";
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8787";

    public static final class ApiException extends Exception {
        public final int status;
        public final String code;

        ApiException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    private final SharedPreferences prefs;
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String email;
    private String displayName;

    public CloudApi(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        accessToken = prefs.getString(KEY_ACCESS, null);
        refreshToken = prefs.getString(KEY_REFRESH, null);
        userId = prefs.getString(KEY_USER_ID, null);
        email = prefs.getString(KEY_EMAIL, null);
        displayName = prefs.getString(KEY_NAME, null);
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    public void setBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("/+$", "");
        prefs.edit().putString(KEY_BASE_URL, normalized.isEmpty() ? DEFAULT_BASE_URL : normalized).apply();
    }

    public boolean isSignedIn() {
        return accessToken != null && refreshToken != null;
    }

    public String getUserEmail() {
        return email == null ? "" : email;
    }

    public String getDisplayName() {
        return displayName == null || displayName.isEmpty() ? "Creator" : displayName;
    }

    public void clearSession() {
        accessToken = null;
        refreshToken = null;
        userId = null;
        email = null;
        displayName = null;
        prefs.edit()
                .remove(KEY_ACCESS)
                .remove(KEY_REFRESH)
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_NAME)
                .apply();
    }

    public JSONObject register(String address, String password, String name) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", address);
        body.put("password", password);
        body.put("displayName", name);
        JSONObject root = requestRaw("POST", "/v1/auth/register", body, false);
        acceptSession(root);
        return root.optJSONObject("user");
    }

    public JSONObject login(String address, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", address);
        body.put("password", password);
        JSONObject root = requestRaw("POST", "/v1/auth/login", body, false);
        acceptSession(root);
        return root.optJSONObject("user");
    }

    public JSONObject me() throws Exception {
        JSONObject root = request("GET", "/v1/auth/me", null, true);
        JSONObject user = root.optJSONObject("user");
        if (user != null) {
            email = user.optString("email", email);
            displayName = user.optString("displayName", displayName);
            saveIdentity();
        }
        return user;
    }

    public void logout() {
        try {
            if (isSignedIn()) {
                requestRaw("POST", "/v1/auth/logout", new JSONObject(), true);
            }
        } catch (Exception ignored) {
        } finally {
            clearSession();
        }
    }

    public JSONObject models() throws Exception {
        return request("GET", "/v1/music/models", null, false);
    }

    public JSONArray catalog(String query) throws Exception {
        String suffix = "/v1/music/tracks?source=licensed&limit=30";
        if (query != null && !query.trim().isEmpty()) {
            suffix += "&q=" + URLEncoder.encode(query.trim(), "UTF-8");
        }
        JSONObject root = request("GET", suffix, null, false);
        return root.optJSONArray("tracks");
    }

    public JSONArray library() throws Exception {
        JSONObject root = request("GET", "/v1/music/library?limit=50", null, true);
        return root.optJSONArray("tracks");
    }

    public JSONObject createTrack(String sourceType, JSONObject input) throws Exception {
        JSONObject body = new JSONObject();
        body.put("sourceType", sourceType);
        body.put("input", input);
        JSONObject root = request("POST", "/v1/music/tracks", body, true);
        JSONObject track = root.optJSONObject("track");
        if (track == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return a track.");
        }
        return track;
    }

    public JSONObject presignUpload(String kind, String contentType, long sizeBytes) throws Exception {
        JSONObject body = new JSONObject();
        body.put("kind", kind);
        body.put("contentType", contentType);
        body.put("sizeBytes", sizeBytes);
        JSONObject root = request("POST", "/v1/uploads/presign", body, true);
        JSONObject upload = root.optJSONObject("upload");
        if (upload == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return an upload URL.");
        }
        return upload;
    }

    public void upload(String url, String contentType, InputStream input, long sizeBytes) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(sizeBytes);
            connection.setRequestProperty("Content-Type", contentType);
            try (OutputStream output = connection.getOutputStream()) {
                copy(input, output, sizeBytes);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String body = readText(connection.getErrorStream());
                throw new ApiException(status, "UPLOAD_FAILED",
                        body.isEmpty() ? "Storage rejected the upload." : body);
            }
        } finally {
            connection.disconnect();
        }
    }

    public JSONObject createProject(JSONObject body) throws Exception {
        JSONObject root = request("POST", "/v1/projects", body, true);
        JSONObject project = root.optJSONObject("project");
        if (project == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return a project.");
        }
        return project;
    }

    public JSONObject createRender(String projectId) throws Exception {
        JSONObject root = request("POST", "/v1/projects/" + projectId + "/render", new JSONObject(), true);
        JSONObject job = root.optJSONObject("job");
        if (job == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return a render job.");
        }
        return job;
    }

    public JSONObject renderJob(String jobId) throws Exception {
        JSONObject root = request("GET", "/v1/render-jobs/" + jobId, null, true);
        JSONObject job = root.optJSONObject("job");
        if (job == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return a render job.");
        }
        return job;
    }

    public JSONObject cancelRender(String jobId) throws Exception {
        JSONObject root = request("POST", "/v1/render-jobs/" + jobId + "/cancel", new JSONObject(), true);
        JSONObject job = root.optJSONObject("job");
        if (job == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return a render job.");
        }
        return job;
    }

    public JSONObject retryRender(String jobId) throws Exception {
        JSONObject root = request("POST", "/v1/render-jobs/" + jobId + "/retry", new JSONObject(), true);
        JSONObject job = root.optJSONObject("job");
        if (job == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server did not return a render job.");
        }
        return job;
    }

    public static String errorMessage(Throwable error) {
        if (error instanceof ApiException) {
            return error.getMessage();
        }
        if (error instanceof SocketTimeoutException) {
            return "Cloud request timed out.";
        }
        return "Cloud unavailable. Check the server URL and connection.";
    }

    private JSONObject request(String method, String path, JSONObject body, boolean auth) throws Exception {
        try {
            return requestRaw(method, path, body, auth);
        } catch (ApiException error) {
            if (auth && error.status == 401 && refreshToken != null) {
                refreshSession();
                return requestRaw(method, path, body, true);
            }
            throw error;
        }
    }

    private synchronized void refreshSession() throws Exception {
        if (refreshToken == null) {
            throw new ApiException(401, "UNAUTHORIZED", "Sign in again to continue.");
        }
        JSONObject body = new JSONObject();
        body.put("refreshToken", refreshToken);
        try {
            JSONObject root = requestRaw("POST", "/v1/auth/refresh", body, false);
            acceptSession(root);
        } catch (Exception error) {
            clearSession();
            throw new ApiException(401, "UNAUTHORIZED", "Your session expired. Sign in again.");
        }
    }

    private JSONObject requestRaw(String method, String path, JSONObject body, boolean auth) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(getBaseUrl() + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");
            if (auth) {
                if (accessToken == null) {
                    throw new ApiException(401, "UNAUTHORIZED", "Sign in to continue.");
                }
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            if (body != null) {
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = readText(stream);
            JSONObject payload;
            try {
                payload = text.isEmpty() ? new JSONObject() : new JSONObject(text);
            } catch (Exception parseError) {
                payload = new JSONObject();
            }

            if (status < 200 || status >= 300) {
                JSONObject error = payload.optJSONObject("error");
                String message = error == null ? "Cloud request failed." : error.optString("message");
                String code = error == null ? "REQUEST_FAILED" : error.optString("code", "REQUEST_FAILED");
                throw new ApiException(status, code, message);
            }
            return payload;
        } finally {
            connection.disconnect();
        }
    }

    private void acceptSession(JSONObject root) throws Exception {
        JSONObject tokens = root.optJSONObject("tokens");
        JSONObject user = root.optJSONObject("user");
        if (tokens == null || user == null) {
            throw new ApiException(500, "INVALID_RESPONSE", "The server returned an invalid session.");
        }
        accessToken = tokens.optString("accessToken", null);
        refreshToken = tokens.optString("refreshToken", null);
        userId = user.optString("id", null);
        email = user.optString("email", null);
        displayName = user.optString("displayName", null);
        prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, displayName)
                .apply();
    }

    private void saveIdentity() {
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, displayName)
                .apply();
    }

    private static String readText(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void copy(InputStream input, OutputStream output, long expected) throws Exception {
        byte[] buffer = new byte[16384];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > expected) {
                throw new ApiException(400, "UPLOAD_SIZE_MISMATCH", "Audio file changed during upload.");
            }
            output.write(buffer, 0, read);
        }
        if (total != expected) {
            throw new ApiException(400, "UPLOAD_SIZE_MISMATCH", "Audio file size did not match.");
        }
    }
}
