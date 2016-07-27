package com.zl.pokemap.betterpokemap.auth;

import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;

//API changed, i dont want to deal with it right now
public class CredentialProviderAdapter extends CredentialProvider {

    private final RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo authInfo;
    public CredentialProviderAdapter(RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo authInfo){
        this.authInfo = authInfo;
    }

    @Override
    public String getTokenId() throws LoginFailedException, RemoteServerException {
        return "";
    }

    @Override
    public RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo getAuthInfo() throws LoginFailedException, RemoteServerException {
        return authInfo;
    }

    @Override
    public boolean isTokenIdExpired() {
        return false;
    }
}
