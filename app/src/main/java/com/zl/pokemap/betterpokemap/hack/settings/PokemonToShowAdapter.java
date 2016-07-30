package com.zl.pokemap.betterpokemap.hack.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.zl.pokemap.betterpokemap.R;
import com.zl.pokemap.betterpokemap.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;

/**
 * Custom adapter to show pokemon and their icons in the preferences screen.
 * <p>
 * Created by fess on 26.07.16.
 */
class PokemonToShowAdapter extends BaseAdapter {

    private LayoutInflater mInflater;

    private final List<CharSequence> mEntries = new ArrayList<>();

    private final Set<PokemonIdOuterClass.PokemonId> mSelected = new HashSet<>();
    private final List<PokemonIdOuterClass.PokemonId> sortedList;

    private boolean useHires = true;
    PokemonToShowAdapter(Context context,
                         CharSequence[] entries,
                         List<PokemonIdOuterClass.PokemonId> sortedList) {
        this.sortedList = sortedList;
        Collections.addAll(mEntries, entries);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        useHires = prefs.getBoolean("use_hires", true);

        mInflater = LayoutInflater.from(context);
        PokemapAppPreferences mPref = new PokemapSharedPreferences(context);
        System.out.println(mPref.getShowablePokemonIDs());
        mSelected.addAll(mPref.getShowablePokemonIDs());
    }

    Set<PokemonIdOuterClass.PokemonId> getShowablePokemonIDs() {
        return mSelected;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View row = view;
        CustomHolder holder;

        if (row == null) {
            row = mInflater.inflate(R.layout.item_pokemon_to_show_preference, viewGroup, false);
            holder = new CustomHolder(row);
        } else {
            holder = (CustomHolder) row.getTag();
        }

        holder.bind(row, position);
        row.setTag(holder);

        return row;
    }

    private class CustomHolder {
        private CheckedTextView mCheckableTextView = null;
        private ImageView mImageView = null;

        CustomHolder(View row) {
            mCheckableTextView = (CheckedTextView) row.findViewById(R.id.textView);
            mImageView = (ImageView) row.findViewById(R.id.imageView);
        }

        void bind(final View row, final int position) {
            PokemonIdOuterClass.PokemonId pokemonId = PokemonIdOuterClass.PokemonId.UNRECOGNIZED;
            if(sortedList.size()>position){
                pokemonId = sortedList.get(position);
            }



            mCheckableTextView.setText((CharSequence) getItem(position));
            mCheckableTextView.setChecked(mSelected.contains(pokemonId));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PokemonIdOuterClass.PokemonId pokemonId = PokemonIdOuterClass.PokemonId.UNRECOGNIZED;
                    if(sortedList.size()>position){
                        pokemonId = sortedList.get(position);
                    }
                    if (mSelected.contains(pokemonId)) {
                        mSelected.remove(pokemonId);
                    } else {
                        mSelected.add(pokemonId);
                    }
                    mCheckableTextView.setChecked(mSelected.contains(pokemonId));
                }
            });

            int resourceID = Utils.getPokemonResourceId(mImageView.getContext(), useHires, pokemonId.getNumber());
            mImageView.setImageResource(resourceID);

        }
    }
}