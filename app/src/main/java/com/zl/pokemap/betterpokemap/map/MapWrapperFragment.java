package com.zl.pokemap.betterpokemap.map;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.SphericalUtil;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.vincentbrison.openlibraries.android.dualcache.Builder;
import com.vincentbrison.openlibraries.android.dualcache.CacheSerializer;
import com.vincentbrison.openlibraries.android.dualcache.DualCache;
import com.zl.pokemap.betterpokemap.PokeMapsActivity;
import com.zl.pokemap.betterpokemap.PokemonCatcher;
import com.zl.pokemap.betterpokemap.R;
import com.zl.pokemap.betterpokemap.Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import POGOProtos.Map.Fort.FortLureInfoOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.support.design.widget.Snackbar.make;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LocationRequestListener} interface
 * to handle interaction events.
 * Use the {@link MapWrapperFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapWrapperFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnInfoWindowClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 703;

    private LocationRequestListener mListener;

    private View mView;
    private TextView mTimer;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mGoogleMap;

    public MapWrapperFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapWrapperFragment.
     */
    public static MapWrapperFragment newInstance() {
        MapWrapperFragment fragment = new MapWrapperFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    private DualCache<String> pokemonCache;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CacheSerializer<String> serializer = new CacheSerializer<String>() {
            @Override
            public String fromString(String data) {
                return data;
            }

            @Override
            public String toString(String object) {
                return object;
            }
        };
        pokemonCache = new Builder<>(getActivity().getPackageName(), 1, String.class)
                .useSerializerInRam(1024, serializer)
                .useSerializerInDisk(2048, true, serializer, getActivity())
                .build();

    }


    private Handler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment if the view is not null
        if (mView == null)
            mView = inflater.inflate(R.layout.fragment_map_wrapper, container, false);
        else {

        }
        mTimer = (TextView) mView.findViewById(R.id.timer);
        handler = new PokemonCatcher.TimeHandler(mTimer);

        // build the map
        if (mSupportMapFragment == null) {
            mSupportMapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mSupportMapFragment).commit();
        }

        if (mGoogleMap == null) {
            mSupportMapFragment.getMapAsync(this);
        }


        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //maybe later
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        PokemonCatcher.lastCatchAttempt = prefs.getLong("last_catch_attempt", 0);

        handler.sendEmptyMessageDelayed(0, 5000);
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeMessages(0);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LocationRequestListener) {
            mListener = (LocationRequestListener) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //if (mGoogleMap == null) mSupportMapFragment.getMapAsync(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnMyLocationButtonClickListener(this);
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        UiSettings settings = mGoogleMap.getUiSettings();
        settings.setTiltGesturesEnabled(false);
        settings.setCompassEnabled(true);
        settings.setTiltGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(true);
        settings.setMapToolbarEnabled(false);



    }

    public void onLocationReady(Location myLocation){
        if(mGoogleMap != null){
            CameraPosition position = mGoogleMap.getCameraPosition();
            if(position.zoom < 13){
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), 15)
                );
            }

        }

    }

    private LatLng getMapCenter(){
        VisibleRegion visibleRegion = mGoogleMap.getProjection()
                .getVisibleRegion();

        Point x = mGoogleMap.getProjection().toScreenLocation(
                visibleRegion.farRight);

        Point y = mGoogleMap.getProjection().toScreenLocation(
                visibleRegion.nearLeft);

        Point centerPoint = new Point(x.x / 2, y.y / 2);

        LatLng centerFromPoint = mGoogleMap.getProjection().fromScreenLocation(
                centerPoint);
        return centerFromPoint;
    }


    @MainThread
    public synchronized boolean checkWait(){
        for(AsyncTask task : currentTasks){
            AsyncTask.Status status = task.getStatus();
            if(!AsyncTask.Status.FINISHED.equals(status)){
                make(mView, R.string.wait, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.stop, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                stopped.set(true);
                                for(AsyncTask task : currentTasks){
                                    task.cancel(true);

                                }
                                currentTasks.clear();
                                ((PokeMapsActivity)getActivity()).hideProgress();
                            }
                        })
                        .show();
                return true;
            }
        }
        return false;
    }

    private ExecutorService executors = Executors.newSingleThreadExecutor();
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private List<AsyncTask> currentTasks = new ArrayList<>();
    private ConcurrentHashMap<Marker, CatchablePokemon> catchablePokemons = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Marker, WildPokemonOuterClass.WildPokemon> pokemons = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Marker, Pokestop> pokestops = new ConcurrentHashMap<>();
    private long lastRefreshAt = 0;
    private int frequentRefreshCount = 0;
    public synchronized   void showPokemon(){

        if(checkWait()){
            return;
        }

        try {
            CameraPosition position = mGoogleMap.getCameraPosition();
            if(position.zoom < 15){
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        position.target, 15)
                );
            }
            final LatLng center = getMapCenter();

            if(center != null){
                stopped.set(false);

                boolean isRefreshTooQuickly = System.currentTimeMillis() - lastRefreshAt < 3*60*1000;
                if(isRefreshTooQuickly){
                    frequentRefreshCount++;
                }
                if(isRefreshTooQuickly && frequentRefreshCount > 2){
                    showError(R.string.frequent_refresh_warning, LENGTH_LONG);
                    frequentRefreshCount = 0;
                }

                lastRefreshAt = System.currentTimeMillis();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final String showPokestop = prefs.getString("show_pokestop", "none");

                List<LatLng> generated = Utils.generateLatLng(center);

                final double maxDistance = SphericalUtil.computeDistanceBetween(
                        center, generated.get(generated.size()-1));

                final AtomicBoolean hasCleared = new AtomicBoolean(false);
                final AtomicInteger count = new AtomicInteger(0);
                currentTasks.clear();

                final Set<String> dedupe = new HashSet<>();
                for(int idx=0;idx<generated.size();idx++){
                    final boolean isLast = idx == generated.size() -1;
                    final LatLng ll = generated.get(idx);
                    AsyncTask at = new AsyncTask() {

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                        }

                        @Override
                        protected Object doInBackground(Object[] objects) {
                            if(stopped.get()){
                               return null;
                            }
                            final PokeMapsActivity pma = (PokeMapsActivity) getActivity();
                            PokemonGo go = pma.getPokemonGo();
                            if(go == null){
                                return null;
                            }

                            pma.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pma.showProgress();
                                    final Circle circle = mGoogleMap.addCircle(new CircleOptions()
                                            .center(ll)
                                            .radius(200)
                                            .strokeColor(Color.parseColor("#CB1D0E"))
                                            .strokeWidth(2)
                                            .fillColor(Color.parseColor("#33CB1D0E")));
                                    mView.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            circle.remove();
                                        }
                                    }, 2000);
                                }
                            });
                            try {

                                if(count.get() > 0){
                                    Thread.sleep(2000);
                                }
                                count.incrementAndGet();

                                LatLng location = (LatLng)objects[0];


                                go.setLocation(location.latitude, location.longitude, 0);

                                java.util.Map<MarkerOptions, WildPokemonOuterClass.WildPokemon> wildPokemonMap = new HashMap<>();
                                for(WildPokemonOuterClass.WildPokemon pokemon : go.getMap().getMapObjects().getWildPokemons()){
                                    if(pokemonCache != null){
                                        String spawnId = pokemonCache.get(String.valueOf( pokemon.getEncounterId()));
                                        if(pokemon.getSpawnPointId().equals(spawnId)){
                                            continue;
                                        }
                                    }

                                    if(!dedupe.contains(pokemon.getSpawnPointId()) && pokemon.getTimeTillHiddenMs() > 0){
                                        dedupe.add(pokemon.getSpawnPointId());
                                        wildPokemonMap.put(new MarkerOptions()
                                                .snippet(getString(R.string.tap_to_catch))
                                                .position(new LatLng(pokemon.getLatitude(), pokemon.getLongitude()))
                                                .title(pokemon.getPokemonData().getPokemonId().name()), pokemon);
                                    }
                                }


                                java.util.Map<MarkerOptions, CatchablePokemon> catchablePokemonMap = new HashMap<>();
//                                for(CatchablePokemon cp : go.getMap().getCatchablePokemon()){
//                                    if(pokemonCache != null){
//                                        String spawnId = pokemonCache.get(String.valueOf( cp.getEncounterId()));
//                                        if(cp.getSpawnPointId().equals(spawnId)){
//                                            continue;
//                                        }
//                                    }
//                                    if(!dedupe.contains(cp.getSpawnPointId())){
//                                        dedupe.add(cp.getSpawnPointId());
//                                        catchablePokemonMap.put(new MarkerOptions()
//                                                .position(new LatLng(cp.getLatitude(), cp.getLongitude()))
//                                                .title(cp.getPokemonId().name()), cp);
//                                    }
//
//                                }

                                Map<MarkerOptions, Pokestop> pokestopMap = new HashMap<>();
                                if(!"none".equalsIgnoreCase(showPokestop)){
                                    for(Pokestop pokestop : go.getMap().getMapObjects().getPokestops()){
                                        if(!dedupe.contains(pokestop.getId())){
                                            dedupe.add(pokestop.getId());
                                            if((getLureExpireAt(pokestop) > 0 || "all".equalsIgnoreCase(showPokestop))
                                                    && SphericalUtil.computeDistanceBetween(center,
                                                    new LatLng(pokestop.getLatitude(), pokestop.getLongitude())) <= maxDistance){
                                                pokestopMap.put(new MarkerOptions()
                                                        .position(new LatLng(pokestop.getLatitude(), pokestop.getLongitude()))
                                                        .title(pokestop.getDetails().getName()), pokestop);
                                            }

                                        }
                                    }
                                }


                                Container ret = new Container();
                                ret.catchablePokemonMap = catchablePokemonMap;
                                ret.wildPokemonMap = wildPokemonMap;
                                ret.pokestopMap = pokestopMap;

                                return ret;
                            }catch (LoginFailedException le){
                                stopped.set(true);
                                ((PokeMapsActivity) getActivity()).askLoginWorker();
                            }catch (RemoteServerException re){
                                re.printStackTrace();
                                stopped.set(true);
                                showErrorWorker(getString(R.string.server_error_message), Snackbar.LENGTH_INDEFINITE);
                            }catch (Exception e){
                                e.printStackTrace();
                                return e;
                            }

                            return null;
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            super.onPostExecute(o);
                            if(o instanceof Container){
                                try {
                                    StringBuilder sb = new StringBuilder();
                                    java.util.Map<MarkerOptions, CatchablePokemon> markers =
                                            ((Container)o).catchablePokemonMap;
                                    if(markers.size() > 0){
                                        if(!hasCleared.get()){
                                            hasCleared.set(true);
                                            mGoogleMap.clear();
                                            pokemons.clear();
                                            catchablePokemons.clear();
                                            pokestops.clear();
                                        }
                                        for(Map.Entry<MarkerOptions, CatchablePokemon> e : markers.entrySet()){
                                            CatchablePokemon cp = e.getValue();
                                            MarkerOptions mo = e.getKey();
                                            String uri = "p" + cp.getPokemonId().getNumber();
                                            int resourceID = getResources().getIdentifier(uri, "drawable", getContext().getPackageName());
                                            Bitmap image = null;
                                            if(resourceID != 0){
                                                image = BitmapFactory.decodeResource(getResources(), resourceID);
                                                mo.icon(BitmapDescriptorFactory.fromBitmap(image));
                                            }
                                            Marker marker = mGoogleMap.addMarker(mo);
                                            catchablePokemons.put(marker, cp);
                                            sb.append(marker.getTitle()).append(" ");
                                        }
                                        ((PokeMapsActivity)getActivity()).setStatus("Found: "+sb.toString());

                                    }

                                    sb = new StringBuilder();
                                    java.util.Map<MarkerOptions, WildPokemonOuterClass.WildPokemon> wildPokemonMap =
                                            ((Container)o).wildPokemonMap;
                                    if(wildPokemonMap.size() > 0){
                                        if(!hasCleared.get()){
                                            hasCleared.set(true);
                                            mGoogleMap.clear();
                                            pokemons.clear();
                                            catchablePokemons.clear();
                                            pokestops.clear();
                                        }
                                        for(Map.Entry<MarkerOptions, WildPokemonOuterClass.WildPokemon> e : wildPokemonMap.entrySet()){
                                            WildPokemonOuterClass.WildPokemon pokemon = e.getValue();
                                            MarkerOptions mo = e.getKey();
                                            String uri = "p" + pokemon.getPokemonData().getPokemonId().getNumber();
                                            int resourceID = getResources().getIdentifier(uri, "drawable", getContext().getPackageName());
                                            Bitmap image = null;
                                            if(resourceID != 0){
                                                image = BitmapFactory.decodeResource(getResources(), resourceID);
                                                mo.icon(BitmapDescriptorFactory.fromBitmap(image));
                                            }
                                            Marker marker = mGoogleMap.addMarker(mo);

                                            pokemons.put(marker, pokemon);
                                            sb.append(marker.getTitle()).append(" ");
                                        }
                                        ((PokeMapsActivity)getActivity()).setStatus("Found: "+sb.toString());
                                    }

                                    Map<MarkerOptions, Pokestop> pokestopMap =
                                            ((Container)o).pokestopMap;

                                    for(Map.Entry<MarkerOptions, Pokestop> e : pokestopMap.entrySet()){
                                        if(!hasCleared.get()){
                                            hasCleared.set(true);
                                            mGoogleMap.clear();
                                            pokemons.clear();
                                            catchablePokemons.clear();
                                            pokestops.clear();
                                        }
                                        Pokestop pokestop = e.getValue();
                                        MarkerOptions mo = e.getKey();

                                        long lureExpire = getLureExpireAt(pokestop);
                                        Bitmap image = BitmapFactory.decodeResource(getResources(),
                                                lureExpire > System.currentTimeMillis()? R.drawable.lure : R.drawable.pokestop);
                                        mo.icon(BitmapDescriptorFactory.fromBitmap(image));
                                        Marker marker = mGoogleMap.addMarker(mo);
                                        pokestops.put(marker, pokestop);
                                    }

                                }catch (Exception e){
                                    e.printStackTrace();
                                    showError(e.getMessage());
                                }
                            }else if(o instanceof  Exception){
                                showError(((Exception)o).getMessage());
                            }

                            if(isLast){
                                ((PokeMapsActivity)getActivity()).hideProgress();
                            }

                        }
                    };

                    currentTasks.add(at.executeOnExecutor(executors, ll));

                }

            }
        }catch (Exception e){
            e.printStackTrace();
            showError(e.getMessage());
        }



    }

    private long getLureExpireAt(Pokestop pokestop){
        //doesnt work

        long lureExpire = -1;
        try {
            FortLureInfoOuterClass.FortLureInfo lure = pokestop.getFortData().getLureInfo();
            if(lure != null){
                lureExpire = lure.getLureExpiresTimestampMs();
            }
        }catch (Exception ee){}
        return lureExpire;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if(checkWait()){
            return;
        }
        try{
            final PokeMapsActivity pma = (PokeMapsActivity) getActivity();
            PokemonGo go = pma.getPokemonGo();
            final WildPokemonOuterClass.WildPokemon pokemon = pokemons.get(marker);
            if(pokemon != null){
                new PokemonCatcher().tryCatchPokemon(this, go, pokemon, marker);
            }
        }catch (Exception e){
            showError(e.getMessage());
        }

    }

    private static class Container{
        Map<MarkerOptions, CatchablePokemon> catchablePokemonMap;
        Map<MarkerOptions, WildPokemonOuterClass.WildPokemon> wildPokemonMap;
        Map<MarkerOptions, Pokestop> pokestopMap;
    }


    @MainThread
    public void showError(@StringRes int e){
        showError(e, Snackbar.LENGTH_SHORT);
    }

    @MainThread
    public void showError(@StringRes int e, int length){
        final Snackbar sb = Snackbar.make(mView, e, length);
        if(length == Snackbar.LENGTH_INDEFINITE){
            sb.setAction("ok", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sb.dismiss();
                    Snackbar.make(mView, R.string.try_different_account, Snackbar.LENGTH_SHORT).show();
                }
            });
        }
        sb.show();
    }

    @MainThread
    public void showError(String e){
        showError(e, Snackbar.LENGTH_SHORT);
    }
    @MainThread
    public void showError(String e, int length){
        final Snackbar sb = Snackbar.make(mView, e, length);
        if(length == Snackbar.LENGTH_INDEFINITE){
            sb.setAction("ok", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sb.dismiss();
                    Snackbar.make(mView, R.string.try_different_account, Snackbar.LENGTH_SHORT).show();
                }
            });
        }
        sb.show();
    }

    public void showErrorWorker(final String e, final int length){
        if(mView!=null){
            mView.post(new Runnable() {
                @Override
                public void run() {
                    showError(e, length);
                }
            });
        }
    }


    private boolean checkPermission(String permission, int requestCode) {
        if (ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {

            // Request them if not enabled
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);

            return false;
        } else {
            // do the necessary dank shit
            switch (permission) {
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                case Manifest.permission.ACCESS_FINE_LOCATION:
                    mGoogleMap.setMyLocationEnabled(true);
                    break;

            }

            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // TODO: test all this shit on a 6.0+ phone lmfao

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mGoogleMap.setMyLocationEnabled(true);
                }
                break;

        }

        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onMyLocationButtonClick() {

        return false;
    }

    private long timeTillHidden(WildPokemonOuterClass.WildPokemon pokemon){
        long elapsed = System.currentTimeMillis() - pokemon.getLastModifiedTimestampMs();
        long timeTilHidden = pokemon.getTimeTillHiddenMs();
        if(elapsed > 0){
            timeTilHidden -= elapsed;
        }

        return  timeTilHidden;
    }

    private long timeTillHidden(CatchablePokemon cp){
        long timeTilHidden = System.currentTimeMillis() - cp.getExpirationTimestampMs();
        return  timeTilHidden;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
//        final CatchablePokemon cp = catchablePokemons.get(marker);
        DateTime nowUtc = new DateTime( DateTimeZone.UTC );

        final WildPokemonOuterClass.WildPokemon pokemon = pokemons.get(marker);
        nowUtc = new DateTime( DateTimeZone.UTC );
        if(pokemon != null){
            long timeTilHidden = timeTillHidden(pokemon);

            if(timeTilHidden > 0){
                Duration d = new Duration(timeTilHidden);
                make(mView,
                        MessageFormat.format(
                        getString(R.string.pokemon_info), marker.getTitle(),
                                d.getStandardMinutes(),
                                d.getStandardSeconds() - 60*d.getStandardMinutes()
                        ), Snackbar.LENGTH_LONG)
                        .setAction(R.string.remove, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                marker.remove();
                                pokemons.remove(marker);
                                if(pokemonCache != null){
                                    pokemonCache.put(String.valueOf(pokemon.getEncounterId()), pokemon.getSpawnPointId());
                                }
                            }
                        })
                        .show();
            }else{
                make(mView,
                        MessageFormat.format(
                        getString(R.string.escaped_message), marker.getTitle()),
                        Snackbar.LENGTH_LONG)
                        .show();
                mView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            marker.remove();
                            pokemons.remove(marker);
                        }catch (Exception e){}
                    }
                }, 1000);
            }
            return false;

        }

        final Pokestop pokestop = pokestops.get(marker);
        if(pokestop != null){
            long lureExpire = getLureExpireAt(pokestop) - System.currentTimeMillis();
            if(lureExpire > 0){
                Duration d = new Duration(lureExpire);
                Snackbar.make(mView,
                        MessageFormat.format("Lure expires in {0} mins {1} secs",
                                d.getStandardMinutes(),
                                d.getStandardSeconds() - 60*d.getStandardMinutes()
                        ), Snackbar.LENGTH_LONG)
                        .show();
            }else{
                Snackbar.make(mView, "Latest lure ended, it may have more", Snackbar.LENGTH_LONG)
                        .show();
            }
        }
        return false;
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
//        ((PokeMapsActivity)getActivity()).setCameraLocation(cameraPosition.target);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface LocationRequestListener {
        Location requestLocation();
    }
}

