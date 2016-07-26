package com.zl.pokemap.betterpokemap.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokegoapi.auth.Login;
import com.pokegoapi.auth.PtcAuthJson;
import com.pokegoapi.exceptions.LoginFailedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo;
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo.Builder;
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo.JWT;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PmPTCLogin extends Login {
    public static final String CLIENT_SECRET = "w8ScCUXJQc6kXKw8FiOhd8Fixzht18Dq3PEVkUCP5ZPxtgyWsbTvWHFLm2wNY0JR";
    public static final String REDIRECT_URI = "https://www.nianticlabs.com/pokemongo/error";
    public static final String CLIENT_ID = "mobile-app_pokemon-go";
    public static final String API_URL = "https://pgorelease.nianticlabs.com/plfe/rpc";
    public static final String LOGIN_URL = "https://sso.pokemon.com/sso/login?service=https%3A%2F%2Fsso.pokemon.com%2Fsso%2Foauth2.0%2FcallbackAuthorize";
    public static final String LOGIN_OAUTH = "https://sso.pokemon.com/sso/oauth2.0/accessToken";
    public static final String USER_AGENT = "niantic";
    private final OkHttpClient client;
    private final Context context;
    String token;

    public PmPTCLogin(OkHttpClient client, Context context) {
        CookieJar tempJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap();

            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                this.cookieStore.put(url.host(), cookies);
            }

            public List<Cookie> loadForRequest(HttpUrl url) {
                List cookies = (List)this.cookieStore.get(url.host());
                return (List)(cookies != null?cookies:new ArrayList());
            }
        };
        this.client = client.newBuilder().cookieJar(tempJar).addInterceptor(new Interceptor() {
            public Response intercept(Chain chain) throws IOException {
                Request req = chain.request();
                req = req.newBuilder().header("User-Agent", "niantic").build();
                return chain.proceed(req);
            }
        }).build();
        this.context = context;
    }

    public AuthInfo login(String token) {
        Builder builder = AuthInfo.newBuilder();
        builder.setProvider("ptc");
        builder.setToken(JWT.newBuilder().setContents(token).setUnknown2(59).build());
        AuthInfo auth = builder.build();

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed=mPrefs.edit();
        Gson gson = new Gson();
        ed.putString("auth", gson.toJson(auth));
        ed.commit();
        return auth;
    }

    public AuthInfo login(String username, String password) throws LoginFailedException {
        try {
            Request e = (new okhttp3.Request.Builder()).url("https://sso.pokemon.com/sso/login?service=https%3A%2F%2Fsso.pokemon.com%2Fsso%2Foauth2.0%2FcallbackAuthorize").get().build();
            Response getResponse = this.client.newCall(e).execute();
            Gson gson = (new GsonBuilder()).create();
            PtcAuthJson ptcAuth = (PtcAuthJson)gson.fromJson(getResponse.body().string(), PtcAuthJson.class);
            HttpUrl url = HttpUrl.parse("https://sso.pokemon.com/sso/login?service=https%3A%2F%2Fsso.pokemon.com%2Fsso%2Foauth2.0%2FcallbackAuthorize").newBuilder().addQueryParameter("lt", ptcAuth.getLt()).addQueryParameter("execution", ptcAuth.getExecution()).addQueryParameter("_eventId", "submit").addQueryParameter("username", username).addQueryParameter("password", password).build();
            RequestBody reqBody = RequestBody.create((MediaType)null, new byte[0]);
            Request postRequest = (new okhttp3.Request.Builder()).url(url).method("POST", reqBody).build();
            Response response = this.client.newBuilder().followRedirects(false).followSslRedirects(false).build().newCall(postRequest).execute();
            String body = response.body().string();
            if(body.length() > 0) {

                Map ticket = gson.fromJson(body, Map.class);
                if(ticket.get("errors") != null) {
                    throw new LoginFailedException(String.valueOf(ticket.get("errors")));
                }
            }

            String accessToken = "";
            String authbuilder;
            for(Iterator token = response.headers("location").iterator(); token.hasNext(); accessToken = authbuilder.split("ticket=")[1]) {
                authbuilder = (String)token.next();
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("auth_provider", "ptc")
                    .putString("token", accessToken)
                    .putString("username", username)
                    .commit();


            return login(oauth(username, accessToken));
        } catch (Exception e) {
            throw new LoginFailedException(e.getMessage());
        }
    }


    private static final long T90MINS = 90*60*60*1000L;
    public String oauth(String username, String accessToken) throws Exception {
        RequestBody reqBody = RequestBody.create((MediaType)null, new byte[0]);
        HttpUrl url = HttpUrl.parse("https://sso.pokemon.com/sso/oauth2.0/accessToken").newBuilder().addQueryParameter("client_id", "mobile-app_pokemon-go").addQueryParameter("redirect_uri", "https://www.nianticlabs.com/pokemongo/error").addQueryParameter("client_secret", "w8ScCUXJQc6kXKw8FiOhd8Fixzht18Dq3PEVkUCP5ZPxtgyWsbTvWHFLm2wNY0JR").addQueryParameter("grant_type", "refresh_token").addQueryParameter("code", accessToken).build();
        Request postRequest = (new okhttp3.Request.Builder()).url(url).method("POST", reqBody).build();
        Response response = this.client.newCall(postRequest).execute();
        String body = response.body().string();

        String token1 = body.split("token=")[1];
        token1 = token1.split("&")[0];

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String expires = body.split("expires=")[1];
            long duration = Math.min(T90MINS, Long.parseLong(expires)*1000); //seconds?
            prefs.edit().putLong("expiry", System.currentTimeMillis()+duration)
                    .commit();
        }catch (Exception e){
            e.printStackTrace();
            prefs.edit().remove("expiry");
        }

        return token1;
    }



    public String getToken() {
        return this.token;
    }
}
