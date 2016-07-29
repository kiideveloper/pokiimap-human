package com.zl.pokemap.betterpokemap.hack;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NiceOkHttpClient extends OkHttpClient{
    public NiceOkHttpClient(){
    }


    @Override
    public Call newCall(Request request) {
        try {
            Thread.sleep(500);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return super.newCall(request);
    }


}
