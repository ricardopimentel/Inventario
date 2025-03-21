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
import com.android.volley.toolbox.Volley;
import com.cyberrocket.inventario.LoginActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class GLPIConnect {
    private RequestQueue mQueue;
    public Context mContext;

    public GLPIConnect(Context context){
        mContext = context;
    }

    public void LoginGLPI(String user, String password, final VolleyResponseListener listener){
        //login no glpi, envia usuário e senha, retorna um arraylist, no index 0 está o session_token (caso dê tudo certo), no index 1 a msg de erro (caso dê errado)
        // Codifica usuário e senha para enviar
        byte[] data = (user+":"+password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final String autorizacao = "Basic " + Base64.encodeToString(data, Base64.DEFAULT);

        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

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
        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

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
        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

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
        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

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
        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

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
        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+ complemento;

        JsonObjectRequest request = new JsonObjectRequest(method, url, postparams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
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

    public void DeleteItem(String complemento, final VolleyResponseListener listener) {
        //instancia objs
        mQueue = Volley.newRequestQueue(mContext);

        // pega URL no banco de dados
        Crud crud = new Crud();
        final String url = crud.SelectItem(mContext, "CONFIG", 1, 1)+ complemento;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.DELETE, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                // Deu certo
                listener.onVolleySuccess(url, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Deu errado
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

        try {
            mQueue.add(request);
        }catch (Exception e){
        }
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
