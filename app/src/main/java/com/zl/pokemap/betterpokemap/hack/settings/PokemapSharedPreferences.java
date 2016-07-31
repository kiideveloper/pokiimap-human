package com.zl.pokemap.betterpokemap.hack.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;

/**
 * Provide convenience methods to access shared preferences
 */
public final class PokemapSharedPreferences implements PokemapAppPreferences {
    private static final String TAG = "PokemapSharedPreference";

    private static final String POKEMONS_TO_SHOW = "pokemons_to_show";
    public static final String STEPS = "search_steps";

    private final SharedPreferences sharedPreferences;

    public PokemapSharedPreferences(@NonNull Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public int getSteps() {
        return Integer.parseInt(sharedPreferences.getString(STEPS, "5"));
    }

    @Override
    public int getStepDelay() {
        return Integer.parseInt(sharedPreferences.getString("step_delay", "5"));
    }


    public Set<PokemonIdOuterClass.PokemonId> getShowablePokemonIDs() {
        Set<String> showablePokemonStringIDs = sharedPreferences.getStringSet(POKEMONS_TO_SHOW, null);
        if(showablePokemonStringIDs == null) {
            //Provides the filter with all available pokemon if no filter is set.
            showablePokemonStringIDs = new HashSet<>();
            for (PokemonIdOuterClass.PokemonId pokemonId : PokemonIdOuterClass.PokemonId.values()) {
                if(pokemonId != PokemonIdOuterClass.PokemonId.UNRECOGNIZED) {
                    showablePokemonStringIDs.add(String.valueOf(pokemonId.getNumber()));
                }
            }
        }
        Set<PokemonIdOuterClass.PokemonId> showablePokemonIDs = new HashSet<>();
        for (String stringId : showablePokemonStringIDs) {
            showablePokemonIDs.add(PokemonIdOuterClass.PokemonId.forNumber(Integer.valueOf(stringId)));
        }
        return showablePokemonIDs;
    }

    public void setShowablePokemonIDs(Set<PokemonIdOuterClass.PokemonId> ids) {
        Set<String> showablePokemonStringIDs = new HashSet<>();
        for (PokemonIdOuterClass.PokemonId pokemonId : ids) {
            showablePokemonStringIDs.add(String.valueOf(pokemonId.getNumber()));
        }
        sharedPreferences.edit().putStringSet(POKEMONS_TO_SHOW, showablePokemonStringIDs).apply();
    }

}