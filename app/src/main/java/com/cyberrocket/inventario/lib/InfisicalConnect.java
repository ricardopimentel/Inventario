package com.cyberrocket.inventario.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cyberrocket.inventario.VaultSettingsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class InfisicalConnect {
    private final RequestQueue queue;
    private final Context context;
    private static final String BASE_URL = "https://app.infisical.com";
    
    private String clientId = "";
    private String clientSecret = "";
    private String workspaceId = "";
    private String environment = "";
    
    private static String currentAccessToken = null; // Cache

    public InfisicalConnect(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
        loadCredentials();
    }

    private void loadCredentials() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    context,
                    VaultSettingsActivity.PREF_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            this.clientId = prefs.getString(VaultSettingsActivity.KEY_CLIENT_ID, "");
            this.clientSecret = prefs.getString(VaultSettingsActivity.KEY_CLIENT_SECRET, "");
            this.workspaceId = prefs.getString(VaultSettingsActivity.KEY_WORKSPACE_ID, "");
            this.environment = prefs.getString(VaultSettingsActivity.KEY_ENVIRONMENT, "prod");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface VolleyResponseListener {
        void onVolleySuccess(String response);
        void onVolleyFailure(String error);
    }

    public boolean isConfigured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty() && !workspaceId.isEmpty();
    }

    // --- Authentication --- //
    public void ensureAuthenticated(final VolleyResponseListener listener) {
        if (currentAccessToken != null) {
            listener.onVolleySuccess("Already Authenticated");
            return;
        }
        login(listener);
    }

    private void login(final VolleyResponseListener listener) {
        String url = BASE_URL + "/api/v1/auth/universal-auth/login";
        
        try {
            JSONObject payload = new JSONObject();
            payload.put("clientId", clientId);
            payload.put("clientSecret", clientSecret);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                    response -> {
                        try {
                            currentAccessToken = response.getString("accessToken");
                            listener.onVolleySuccess("Authenticated");
                        } catch (JSONException e) {
                            listener.onVolleyFailure("JSON Error parsing token: " + e.getMessage());
                        }
                    },
                    error -> listener.onVolleyFailure("Auth Error: " + (error.networkResponse != null ? error.networkResponse.statusCode : error.getMessage()))
            );
            queue.add(request);
        } catch (JSONException e) {
             listener.onVolleyFailure("JSON Build Error: " + e.getMessage());
        }
    }

    // --- Secret Operations --- //
    
    // Abstract base method to handle auto-authentication
    private void runWithAuth(Runnable action, VolleyResponseListener errorListener) {
        if (!isConfigured()) {
            errorListener.onVolleyFailure("Infisical credentials not configured");
            return;
        }

        ensureAuthenticated(new VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String response) {
                action.run();
            }
            @Override
            public void onVolleyFailure(String error) {
                errorListener.onVolleyFailure(error);
            }
        });
    }

    private String extractError(VolleyError error, String prefix) {
        String msg = prefix;
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String errorBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject errObj = new JSONObject(errorBody);
                if (errObj.has("message")) {
                    msg += ": " + errObj.getString("message");
                } else if (errObj.has("error")) {
                    msg += ": " + errObj.getString("error");
                } else {
                    msg += ": " + errorBody;
                }
            } catch (Exception e) {
                msg += ": " + new String(error.networkResponse.data, StandardCharsets.UTF_8);
            }
        } else {
            msg += ": " + error.getMessage();
        }
        Log.e("InfisicalConnect", msg);
        return msg;
    }

    public void GetSecret(String secretName, final VolleyResponseListener listener) {
        runWithAuth(() -> {
            String url = BASE_URL + "/api/v3/secrets/raw/" + secretName + "?workspaceId=" + workspaceId + "&environment=" + environment + "&secretPath=/";

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    listener::onVolleySuccess,
                    error -> {
                        if(error.networkResponse != null && error.networkResponse.statusCode == 401) {
                            currentAccessToken = null;
                        }
                        listener.onVolleyFailure(extractError(error, "Erro GET Secret"));
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + currentAccessToken);
                    return headers;
                }
            };
            queue.add(request);
        }, listener);
    }

    public void CreateSecret(String secretName, String secretValue, final VolleyResponseListener listener) {
        runWithAuth(() -> {
            String url = BASE_URL + "/api/v3/secrets/raw/" + secretName;

            try {
                JSONObject payload = new JSONObject();
                payload.put("workspaceId", workspaceId);
                payload.put("environment", environment);
                payload.put("secretValue", secretValue);
                payload.put("secretPath", "/");
                payload.put("type", "shared");

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                        response -> listener.onVolleySuccess(response.toString()),
                        error -> listener.onVolleyFailure(extractError(error, "Erro POST Secret"))
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer " + currentAccessToken);
                        return headers;
                    }
                };
                queue.add(request);
            } catch (JSONException e) {
                listener.onVolleyFailure("JSON Error: " + e.getMessage());
            }
        }, listener);
    }

    public void UpdateSecret(String secretName, String secretValue, final VolleyResponseListener listener) {
        runWithAuth(() -> {
            String url = BASE_URL + "/api/v3/secrets/raw/" + secretName;

            try {
                JSONObject payload = new JSONObject();
                payload.put("workspaceId", workspaceId);
                payload.put("environment", environment);
                payload.put("secretValue", secretValue);
                payload.put("secretPath", "/");
                payload.put("type", "shared");

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.PATCH, url, payload,
                        response -> listener.onVolleySuccess(response.toString()),
                        error -> listener.onVolleyFailure(extractError(error, "Erro PATCH Secret"))
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer " + currentAccessToken);
                        return headers;
                    }
                };
                queue.add(request);
            } catch (JSONException e) {
                listener.onVolleyFailure("JSON Error: " + e.getMessage());
            }
        }, listener);
    }

    public void DeleteSecret(String secretName, final VolleyResponseListener listener) {
        runWithAuth(() -> {
            String url = BASE_URL + "/api/v3/secrets/raw/" + secretName;

            try {
                JSONObject payload = new JSONObject();
                payload.put("workspaceId", workspaceId);
                payload.put("environment", environment);
                payload.put("secretPath", "/");
                payload.put("type", "shared");

                String jsonPayload = payload.toString();
                
                StringRequest request = new StringRequest(Request.Method.DELETE, url,
                        listener::onVolleySuccess,
                        error -> listener.onVolleyFailure(extractError(error, "Erro DELETE Secret"))
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer " + currentAccessToken);
                        headers.put("Content-Type", "application/json");
                        return headers;
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return jsonPayload.getBytes(StandardCharsets.UTF_8);
                    }
                };
                queue.add(request);
            } catch (JSONException e) {
                listener.onVolleyFailure("JSON Error: " + e.getMessage());
            }
        }, listener);
    }
}
