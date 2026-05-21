package com.cyberrocket.inventario.lib;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cyberrocket.inventario.LoginActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Base64;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.android.volley.toolbox.HurlStack;

public class GLPIConnect {
    private RequestQueue mQueue;
    public Context mContext;

    public GLPIConnect(Context context){
        mContext = context;
        mQueue = Volley.newRequestQueue(mContext, new HurlStack(null, getUnsafeSocketFactory()));
    }

    private SSLSocketFactory getUnsafeSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            
            return sc.getSocketFactory();
        } catch (Exception e) {
            return null;
        }
    }

    public void LoginGLPI(String user, String password, final VolleyResponseListener listener){
        //login no glpi, envia usuário e senha, retorna um arraylist, no index 0 está o session_token (caso dê tudo certo), no index 1 a msg de erro (caso dê errado)
        // Codifica usuário e senha para enviar
        byte[] data = (user+":"+password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final String autorizacao = "Basic " + Base64.encodeToString(data, Base64.DEFAULT);


        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+"/apirest.php/initSession";

        //Cria requisição
        JsonObjectRequest jsonObjRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject equipamento) { //entra se receber resposta do servidor
                        listener.onVolleySuccess(url, equipamento.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {// entra se não receber resposta válida do servidor
                String msg = "";
                try { //entra caso a resposta possa ser decodificada
                    msg = new String(error.networkResponse.data, "utf-8");
                } catch (Exception e) { // entra caso a resposta não possa ser decodificada
                    msg = error.toString();
                }
               listener.onVolleyFailure(msg);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Authorization", autorizacao);
                return params;
            }
        };

        mQueue.add(jsonObjRequest);
    }

    public void LogoffGLPI(final VolleyResponseListener listener){
        //logoff no glpi, envia o token para remover do sistema

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+"/apirest.php/killSession";

        //Cria requisição
        JsonObjectRequest jsonObjRequest = new CustomJsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) { //entra se receber resposta do servidor
                listener.onVolleySuccess(url, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onVolleyFailure(error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo

                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                Log.d("Endereco", "SessionToken: "+ crud.SelectItem(mContext, "CONFIG", 1, 2));
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }
        };
        try {
            mQueue.add(jsonObjRequest);
        }catch (Exception e){
        }
    }

    public void GetItem(String complemento, final VolleyResponseListener listener){

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+complemento;

        JsonObjectRequest jsonObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //
                listener.onVolleySuccess(url, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Deu errado
                if (error instanceof AuthFailureError) { //Se O erro for de autenticação, redireciona para a tela de login
                    SairSistema();
                }
                listener.onVolleyFailure(error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo

                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }
        };

        mQueue.add(jsonObjRequest);
    }

    public void GetArray(String complemento, final VolleyResponseListener listener) {
        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+complemento;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                // Deu certo
                Log.d("SessionToken", "SessionToken: "+ crud.SelectItem(mContext, "CONFIG", 1, 2));
                listener.onVolleySuccess(url, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("SessionToken", "Deu erro aqui: "+crud.SelectItem(mContext, "CONFIG", 1, 2));
                //Deu errado
                if (error instanceof AuthFailureError) { //Se O erro for de autenticação, redireciona para a tela de login
                    Log.d("SessionToken", "Mandei sair do sistema");
                    SairSistema();
                }
                listener.onVolleyFailure(error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo

                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }
        };

        mQueue.add(request);
    }

    public void UpdateItem(String complemento, JSONObject postparams, int method, final VolleyResponseListener listener) {

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+ complemento;

        CustomJsonObjectToArrayRequest request = new CustomJsonObjectToArrayRequest(method, url, postparams, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                // Deu certo
                listener.onVolleySuccess(url, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Deu errado
                if (error instanceof AuthFailureError) { //Se O erro for de autenticação, redireciona para a tela de login
                    SairSistema();
                }
                listener.onVolleyFailure(error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo

                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }
        };

        mQueue.add(request);
    }

    public void InsertItem(String complemento, JSONObject postparams, int method, final VolleyResponseListener listener) {

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+ complemento;
        final String requestBody = postparams.toString();

        StringRequest request = new StringRequest(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Deu certo
                listener.onVolleySuccess(url, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Deu errado
                if (error instanceof AuthFailureError) { //Se O erro for de autenticação, redireciona para a tela de login
                    SairSistema();
                }
                String msg = "";
                try {
                    if(error.networkResponse != null && error.networkResponse.data != null) {
                        msg = new String(error.networkResponse.data, "utf-8");
                    } else {
                        msg = error.toString();
                    }
                } catch (Exception e) {
                    msg = error.toString();
                }
                listener.onVolleyFailure(msg);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo

                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody.getBytes("utf-8");
                } catch (java.io.UnsupportedEncodingException uee) {
                    return null;
                }
            }
        };

        mQueue.add(request);
    }

    public void UpdateItemRaw(String complemento, JSONObject postparams, int method, final VolleyResponseListener listener) {

        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+ complemento;
        final String requestBody = postparams.toString();

        StringRequest request = new StringRequest(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onVolleySuccess(url, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof AuthFailureError) {
                    SairSistema();
                }
                String msg = "";
                try {
                    if(error.networkResponse != null && error.networkResponse.data != null) {
                        msg = new String(error.networkResponse.data, "utf-8");
                    } else {
                        msg = error.toString();
                    }
                } catch (Exception e) {
                    msg = error.toString();
                }
                listener.onVolleyFailure(msg);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud();
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody.getBytes("utf-8");
                } catch (java.io.UnsupportedEncodingException uee) {
                    return null;
                }
            }
        };

        mQueue.add(request);
    }

    public void DeleteItem(String complemento, final VolleyResponseListener listener) {

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+ complemento;

        StringRequest request = new StringRequest(Request.Method.DELETE, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Deu certo
                listener.onVolleySuccess(url, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Deu errado
                if (error instanceof AuthFailureError) { //Se O erro for de autenticação, redireciona para a tela de login
                    SairSistema();
                }
                String msg = "";
                try {
                    if(error.networkResponse != null && error.networkResponse.data != null) {
                        msg = new String(error.networkResponse.data, "utf-8");
                    } else {
                        msg = error.toString();
                    }
                } catch (Exception e) {
                    msg = error.toString();
                }
                listener.onVolleyFailure(msg);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo

                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", crud.SelectItem(mContext, "CONFIG", 1, 2));
                return params;
            }
        };

        try {
            mQueue.add(request);
        }catch (Exception e){
        }
    }

    public void UploadDocument(final String fileName, final byte[] fileData, final String mimeType, final VolleyResponseListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Crud crud = new Crud();
                    String baseUrl = crud.SelectItem(mContext, "CONFIG", 1, 1);
                    String sessionToken = crud.SelectItem(mContext, "CONFIG", 1, 2);
                    final String urlStr = baseUrl + "/apirest.php/Document";

                    java.net.URL url = new java.net.URL(urlStr);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                        javax.net.ssl.SSLSocketFactory unsafeSf = getUnsafeSocketFactory();
                        if (unsafeSf != null) {
                            ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(unsafeSf);
                        }
                        ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                                return true;
                            }
                        });
                    }

                    String boundary = "Boundary-" + System.currentTimeMillis();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("User-Agent", "Android Multipart");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    conn.setRequestProperty("Session-Token", sessionToken);

                    java.io.OutputStream out = conn.getOutputStream();
                    java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(out, "UTF-8"), true);

                    // part 1: uploadManifest
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"uploadManifest\"").append("\r\n");
                    writer.append("Content-Type: application/json; charset=UTF-8").append("\r\n");
                    writer.append("\r\n");
                    
                    JSONObject input = new JSONObject();
                    input.put("name", fileName);
                    
                    JSONArray filenames = new JSONArray();
                    filenames.put(fileName);
                    input.put("_filename", filenames);
                    
                    JSONObject manifest = new JSONObject();
                    manifest.put("input", input);
                    
                    writer.append(manifest.toString()).append("\r\n");
                    writer.flush();

                    // part 2: filename[0]
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"filename[0]\"; filename=\"" + fileName + "\"").append("\r\n");
                    writer.append("Content-Type: " + mimeType).append("\r\n");
                    writer.append("\r\n");
                    writer.flush();

                    out.write(fileData);
                    out.flush();

                    writer.append("\r\n");
                    writer.append("--" + boundary + "--").append("\r\n");
                    writer.flush();
                    writer.close();

                    final int responseCode = conn.getResponseCode();
                    if (responseCode == 200 || responseCode == 201) {
                        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();
                        final String resStr = response.toString();
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onVolleySuccess(urlStr, resStr);
                            }
                        });
                    } else {
                        java.io.InputStream errorStream = conn.getErrorStream();
                        final String errStr;
                        if (errorStream != null) {
                            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream, "UTF-8"));
                            StringBuilder errResponse = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null) {
                                errResponse.append(line);
                            }
                            in.close();
                            errStr = "HTTP " + responseCode + ": " + errResponse.toString();
                        } else {
                            errStr = "HTTP " + responseCode;
                        }
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onVolleyFailure(errStr);
                            }
                        });
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onVolleyFailure(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void IrPara(Class para){
        Intent intent = new Intent(mContext, para);
        mContext.startActivity(intent);
    }

    private void SairSistema() {
        //Token de login inválido, rever na marra do banco de dados e redirecionar para a tela de login
        Crud crud = new Crud(); //instancia classe de conexão com bd interno, para buscar o token salvo
        //Alterar no banco de dados
        ContentValues values = new ContentValues();
        values.put("SESSION_TOKEN", "");
        crud.UpdateItem(mContext, "CONFIG", 1, values);
        Log.d("SessionToken", "Tô apagando o token: "+crud.SelectItem(mContext, "CONFIG", 1, 2));
        Toast.makeText(mContext, "Sua sessão expirou, refaça o login", Toast.LENGTH_LONG).show();
        IrPara(LoginActivity.class);
    }

    //Interface para retornar a resposta do servidor
    public interface VolleyResponseListener {

        void onVolleySuccess(String url, String serverResponse);
        void onVolleyFailure(String url);
    }

}
