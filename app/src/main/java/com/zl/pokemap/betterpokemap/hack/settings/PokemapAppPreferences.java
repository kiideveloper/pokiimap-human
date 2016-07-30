package com.zl.pokemap.betterpokemap.hack.settings;

import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;

/**
 * A contract which defines a user's app preferences
 */
public interface PokemapAppPreferences {

    int getSteps();

    /**
     * @return a set of pokemonIDs which can be shown according to the preferences.
     */
    Set<PokemonIdOuterClass.PokemonId> getShowablePokemonIDs();

    void setShowablePokemonIDs(Set<PokemonIdOuterClass.PokemonId> pokemonIDs);

}