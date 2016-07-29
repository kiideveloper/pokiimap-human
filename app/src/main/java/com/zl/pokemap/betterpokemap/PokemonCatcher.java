package com.zl.pokemap.betterpokemap;


import android.os.Message;
import android.support.annotation.MainThread;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.zl.pokemap.betterpokemap.map.MapWrapperFragment;

import POGOProtos.Map.Pokemon.WildPokemonOuterClass;

public class PokemonCatcher {
    //not open sourced to prevent abuse, you should be able to write it on your own
    public static long lastCatchAttempt = 0;

    public void tryLootPokestop(MapWrapperFragment mapWrapperFragment, PokemonGo go, Pokestop pokestop, Marker marker) {
    }

    public static class TimeHandler extends LeakGuardHandlerWrapper<TextView>{
        public TimeHandler(TextView ownerInstance) {
            super(ownerInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }


    @MainThread
    public void tryCatchPokemon(final MapWrapperFragment map, final PokemonGo go,
                                final WildPokemonOuterClass.WildPokemon pokemon, final Marker marker){

    }

}
