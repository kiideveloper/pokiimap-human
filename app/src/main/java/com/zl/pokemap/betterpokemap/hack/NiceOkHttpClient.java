package com.zl.pokemap.betterpokemap.hack;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NiceOkHttpClient extends OkHttpClient{
    public NiceOkHttpClient(){
    }

    private static final int TIMEOUT = 10*1000;
    private static final int DELAY = 500;

    @Override
    public int readTimeoutMillis() {
        return TIMEOUT;
    }

    @Override
    public int connectTimeoutMillis() {
        return TIMEOUT;
    }

    @Override
    public int writeTimeoutMillis() {
        return TIMEOUT;
    }

    @Override
    public Call newCall(Request request) {
        try {
            Thread.sleep(DELAY);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return super.newCall(request);
    }


}
