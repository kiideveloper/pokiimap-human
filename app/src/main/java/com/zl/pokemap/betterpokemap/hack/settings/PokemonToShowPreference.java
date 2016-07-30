package com.zl.pokemap.betterpokemap.hack.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.zl.pokemap.betterpokemap.R;
import com.zl.pokemap.betterpokemap.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;

/**
 * A multi-select list preference which tells which pokemons to show on the map.
 * <p>
 * Created by fess on 26.07.16.
 */
public class PokemonToShowPreference extends MultiSelectListPreference {

    private PokemonToShowAdapter mAdapter;

    private PokemapSharedPreferences mPref;

    private List<PokemonIdOuterClass.PokemonId> all = new ArrayList<>();
    public PokemonToShowPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PokemonToShowPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entriesValues = new ArrayList<>();
        Set<String> defaultValues = new HashSet<>();
        all.clear();
        all.addAll(Arrays.asList(PokemonIdOuterClass.PokemonId.values()));

        all.remove(PokemonIdOuterClass.PokemonId.MISSINGNO);
        all.remove(PokemonIdOuterClass.PokemonId.UNRECOGNIZED);
        Collections.sort(all, new Comparator<PokemonIdOuterClass.PokemonId>() {
            @Override
            public int compare(PokemonIdOuterClass.PokemonId lhs, PokemonIdOuterClass.PokemonId rhs) {
                return lhs.name().compareTo(rhs.name());
            }
        });

        for (PokemonIdOuterClass.PokemonId pokemonId : all) {
            entries.add(Utils.getLocalizedPokemonName(pokemonId.name(), context));
            entriesValues.add(String.valueOf(pokemonId.getNumber()));
            defaultValues.add(String.valueOf(pokemonId.getNumber()));
        }

        setEntries(entries.toArray(new CharSequence[]{}));
        setEntryValues(entriesValues.toArray(new CharSequence[]{}));

        // all pokemons are shown by default
        setDefaultValue(defaultValues);

        mPref = new PokemapSharedPreferences(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        final CharSequence[] entries = getEntries();
        final CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        mAdapter = new PokemonToShowAdapter(getContext(), entries, new ArrayList<>(all) );
        builder.setAdapter(mAdapter, null);
        View header = LayoutInflater.from(getContext()).inflate(R.layout.custom_header, null);
        builder.setCustomTitle(header);
        header.findViewById(R.id.none).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.getShowablePokemonIDs().clear();
                mAdapter.notifyDataSetChanged();
            }
        });
        header.findViewById(R.id.all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.getShowablePokemonIDs().addAll(all);
                mAdapter.notifyDataSetChanged();
            }
        });

    }





    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Set<PokemonIdOuterClass.PokemonId> pokemonIDs = mAdapter.getShowablePokemonIDs();
            mPref.setShowablePokemonIDs(pokemonIDs);
        }
    }
}