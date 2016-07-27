package com.zl.pokemap.betterpokemap.auth;


import com.pokegoapi.exceptions.LoginFailedException;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;

public abstract class Login {
    public abstract RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo login(String username, String password) throws LoginFailedException;
}
