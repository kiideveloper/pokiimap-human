package com.zl.pokemap.betterpokemap.map;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.text.TextUtils;
import android.util.Property;
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
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.vincentbrison.openlibraries.android.dualcache.Builder;
import com.vincentbrison.openlibraries.android.dualcache.CacheSerializer;
import com.vincentbrison.openlibraries.android.dualcache.DualCache;
import com.zl.pokemap.betterpokemap.BuildConfig;
import com.zl.pokemap.betterpokemap.PokeMapsActivity;
import com.zl.pokemap.betterpokemap.PokemonCatcher;
import com.zl.pokemap.betterpokemap.R;
import com.zl.pokemap.betterpokemap.Utils;
import com.zl.pokemap.betterpokemap.hack.MapHelper;
import com.zl.pokemap.betterpokemap.hack.settings.PokemapAppPreferences;
import com.zl.pokemap.betterpokemap.hack.settings.PokemapSharedPreferences;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.io.InterruptedIOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
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
        restartTimer();

    }

    public void restartTimer(){
        if(handler != null && !handler.hasMessages(0)){
            handler.sendEmptyMessageDelayed(0, 5000);
        }

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
                                removeAllCircles();
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
    private ConcurrentHashMap<Marker, MapPokemonOuterClass.MapPokemon> catchablePokemons = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Marker, WildPokemonOuterClass.WildPokemon> pokemons = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Marker, Pokestop> pokestops = new ConcurrentHashMap<>();
    private Queue<Circle> circles = new ConcurrentLinkedQueue<>();
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

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                final String showPokestop = prefs.getString("show_pokestop", "none");
                final boolean useHires = prefs.getBoolean("use_hires", true);
                PokemapAppPreferences pPref = new PokemapSharedPreferences(getContext());
                final Set<PokemonIdOuterClass.PokemonId> showPokemonIds = pPref.getShowablePokemonIDs();
                int steps = pPref.getSteps();

                List<LatLng> generated =
                        MapHelper.getSearchArea(steps, center);//.generateLatLng(center);




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
                                            .radius(70)
                                            .strokeColor(Color.parseColor("#CB1D0E"))
                                            .strokeWidth(2)
                                            .fillColor(Color.parseColor("#33CB1D0E")));
//                                    circles.add(circle);
                                    mView.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            circle.remove();
                                        }
                                    }, 2000);

                                }
                            });
                            try {

                                if(count.get() > 0 && !BuildConfig.DEBUG){
                                    Thread.sleep(500);
                                }
                                count.incrementAndGet();

                                LatLng location = (LatLng)objects[0];


                                go.setLocation(location.latitude, location.longitude, 0);

                                MapObjects mapObjects = go.getMap().getMapObjects();

                                java.util.Map<MarkerOptions, WildPokemonOuterClass.WildPokemon> wildPokemonMap = new HashMap<>();
                                String message = "";
                                for(WildPokemonOuterClass.WildPokemon pokemon : mapObjects.getWildPokemons()){
                                    try {
                                        if(pokemonCache != null){
                                            String spawnId = pokemonCache.get(String.valueOf( pokemon.getEncounterId()));
                                            if(pokemon.getSpawnPointId().equals(spawnId)){
                                                continue;
                                            }
                                        }

                                        if(!dedupe.contains(pokemon.getSpawnPointId()) && pokemon.getTimeTillHiddenMs() > 0){
                                            dedupe.add(pokemon.getSpawnPointId());
                                            if(showPokemonIds.contains(pokemon.getPokemonData().getPokemonId())){
                                                wildPokemonMap.put(new MarkerOptions()
                                                        .snippet(getString(R.string.tap_to_catch))
                                                        .position(new LatLng(pokemon.getLatitude(), pokemon.getLongitude()))
                                                        .title(Utils.getLocalizedPokemonName(pokemon.getPokemonData().getPokemonId().name(), pma)), pokemon);
                                            }
                                        }
                                    }catch (Exception e){
                                        message = e.getMessage();
                                    }

                                }

                                if(!TextUtils.isEmpty(message)){
                                    showErrorWorker(message, Snackbar.LENGTH_SHORT);
                                }

                                message = "";

                                java.util.Map<MarkerOptions, MapPokemonOuterClass.MapPokemon> catchablePokemonMap = new HashMap<>();
                                //not really necessary...
//                                for(MapPokemonOuterClass.MapPokemon cp : go.getMap().getMapObjects().getCatchablePokemons()){
//                                    if(pokemonCache != null){
//                                        String spawnId = pokemonCache.get(String.valueOf( cp.getEncounterId()));
//                                        if(cp.getSpawnPointId().equals(spawnId)){
//                                            continue;
//                                        }
//                                    }
//                                    if(!dedupe.contains(cp.getSpawnPointId())){
//                                        dedupe.add(cp.getSpawnPointId());
//                                        catchablePokemonMap.put(new MarkerOptions()
//                                                .snippet(getString(R.string.tap_to_catch))
//                                                .position(new LatLng(cp.getLatitude(), cp.getLongitude()))
//                                                .title(Utils.getLocalizedPokemonName(cp.getPokemonData().getPokemonId().name(), pma)), cp);
//                                    }
//
//                                }


                                Map<MarkerOptions, Pokestop> pokestopMap = new HashMap<>();

                                boolean shouldShowPokestop = !"none".equalsIgnoreCase(showPokestop);
                                if(shouldShowPokestop){
                                    for(Pokestop pokestop : mapObjects.getPokestops()){
                                        try {
                                            if(!dedupe.contains(pokestop.getId())){
                                                dedupe.add(pokestop.getId());
                                                if(((pokestop.getFortData()!=null && pokestop.getFortData().hasLureInfo())
                                                        || "all".equalsIgnoreCase(showPokestop))
                                                        && SphericalUtil.computeDistanceBetween(center,
                                                        new LatLng(pokestop.getLatitude(), pokestop.getLongitude())) <= maxDistance){
                                                    pokestopMap.put(new MarkerOptions()
                                                            .snippet(getString(R.string.tap_to_loot))
                                                            .position(new LatLng(pokestop.getLatitude(), pokestop.getLongitude()))
                                                            .title("pokestop"), pokestop);
                                                }
                                            }
                                        }catch (Exception e){ message = e.getMessage();}

                                    }
                                }
                                if(!TextUtils.isEmpty(message)){
                                    showErrorWorker(message, Snackbar.LENGTH_SHORT);
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
                                if(re.getCause() instanceof InterruptedIOException){
                                    //it's okay
                                    showErrorWorker(getString(R.string.refresh_interrupted), Snackbar.LENGTH_SHORT);
                                }else{
                                    showErrorWorker(getString(R.string.server_error_message), Snackbar.LENGTH_INDEFINITE);
                                }
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
                                    java.util.Map<MarkerOptions, MapPokemonOuterClass.MapPokemon> markers =
                                            ((Container)o).catchablePokemonMap;
                                    if(markers.size() > 0){
                                        checkAndClear(hasCleared, center, maxDistance);
                                        for(Map.Entry<MarkerOptions, MapPokemonOuterClass.MapPokemon> e : markers.entrySet()){
                                            MapPokemonOuterClass.MapPokemon cp = e.getValue();
                                            MarkerOptions mo = e.getKey();
                                            int resourceID = Utils.getPokemonResourceId(getContext(), useHires, cp.getPokemonId().getNumber());
                                            if(resourceID != 0){
                                                mo.icon(BitmapDescriptorFactory.fromResource(resourceID));
                                            }
                                            mo.alpha(0f);
                                            Marker marker = mGoogleMap.addMarker(mo);
                                            addMarkerAnimated(marker);
                                            catchablePokemons.put(marker, cp);
                                            sb.append(marker.getTitle()).append(" ");
                                        }
                                        ((PokeMapsActivity)getActivity()).setStatus("Found: "+sb.toString());

                                    }

                                    sb = new StringBuilder();
                                    java.util.Map<MarkerOptions, WildPokemonOuterClass.WildPokemon> wildPokemonMap =
                                            ((Container)o).wildPokemonMap;
                                    if(wildPokemonMap.size() > 0){
                                        checkAndClear(hasCleared, center, maxDistance);
                                        for(Map.Entry<MarkerOptions, WildPokemonOuterClass.WildPokemon> e : wildPokemonMap.entrySet()){
                                            WildPokemonOuterClass.WildPokemon pokemon = e.getValue();
                                            MarkerOptions mo = e.getKey();
                                            int resourceID = Utils.getPokemonResourceId(getContext(), useHires, pokemon.getPokemonData().getPokemonId().getNumber());
                                            if(resourceID != 0){
                                                mo.icon(BitmapDescriptorFactory.fromResource(resourceID));
                                            }
                                            mo.alpha(0f);
                                            Marker marker = mGoogleMap.addMarker(mo);
                                            addMarkerAnimated(marker);
                                            pokemons.put(marker, pokemon);
                                            sb.append(marker.getTitle()).append(" ");
                                        }
                                        ((PokeMapsActivity)getActivity()).setStatus("Found: "+sb.toString());
                                    }

                                    Map<MarkerOptions, Pokestop> pokestopMap =
                                            ((Container)o).pokestopMap;

                                    DateTime nowUtc = new DateTime( DateTimeZone.UTC );
                                    for(Map.Entry<MarkerOptions, Pokestop> e : pokestopMap.entrySet()){
                                        checkAndClear(hasCleared, center, maxDistance);
                                        Pokestop pokestop = e.getValue();
                                        MarkerOptions mo = e.getKey();

                                        long lureExpire = timeTillEnd(pokestop, nowUtc);
                                        mo.icon(BitmapDescriptorFactory.fromResource(lureExpire > 0?
                                                (useHires?R.drawable.slure : R.drawable.lure) :
                                                (useHires?R.drawable.spokestop : R.drawable.pokestop) ));
                                        mo.alpha(0f);
                                        Marker marker = mGoogleMap.addMarker(mo);
                                        addMarkerAnimated(marker);
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
                                removeAllCircles();
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

    private void removeAllCircles(){
        long delay = 0;
        for(final Circle circle : circles){
            delay+=200;
            mView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    circle.remove();
                }
            }, delay);
        }
        circles.clear();
    }


    @MainThread
    private void checkAndClear(AtomicBoolean hasCleared, LatLng center, double maxDistance){
        if(!hasCleared.get()){
            hasCleared.set(true);
            mGoogleMap.clear();
            pokemons.clear();
            catchablePokemons.clear();
            pokestops.clear();
            circles.clear();
            if(BuildConfig.DEBUG){
                mGoogleMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(maxDistance)
                        .strokeColor(Color.parseColor("#1E4CBC"))
                        .strokeWidth(2)
                        .fillColor(Color.parseColor("#331E4CBC")));
            }
        }
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
            final Pokestop pokestop = pokestops.get(marker);
            if(pokestop != null){
                Duration d = new Duration(timeTilX(pokestop.getCooldownCompleteTimestampMs()));
                if(d.getMillis() > 0){
                    make(mView,
                            MessageFormat.format(
                                    getString(R.string.pokestop_info),
                                    d.getStandardMinutes(),
                                    d.getStandardSeconds() - 60*d.getStandardMinutes()
                            ), Snackbar.LENGTH_LONG)
                            .show();
                }else{
                    new PokemonCatcher().tryLootPokestop(this, go, pokestop, marker);
                }

            }

//            MapPokemonOuterClass.MapPokemon cp = catchablePokemons.get(marker);
//            if(cp != null){
//                new PokemonCatcher().tryCatchPokemon(this, go, cp, marker);
//            }

        }catch (Exception e){
            showError(e.getMessage());
        }

    }

    private static class Container{
        Map<MarkerOptions, MapPokemonOuterClass.MapPokemon> catchablePokemonMap;
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

    private long timeTillHidden(MapPokemonOuterClass.MapPokemon cp, DateTime nowUtc){
        return timeTilX(cp.getExpirationTimestampMs(), nowUtc);
    }

    private long timeTillEnd(Pokestop pokestop, DateTime nowUtc){
        if(pokestop == null || pokestop.getFortData() == null || !pokestop.getFortData().hasLureInfo() ){ return 0 ; }

        return timeTilX(pokestop.getFortData().getLureInfo().getLureExpiresTimestampMs(), nowUtc);
    }

    private long timeTilX(long timex){
        return timeTilX(timex, new DateTime( DateTimeZone.UTC ));
    }

    private long timeTilX(long timex, DateTime nowUtc){
        Duration d = new Duration(nowUtc, new DateTime(timex, DateTimeZone.UTC));
        return  Math.max(0, d.getMillis());
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        DateTime nowUtc = new DateTime( DateTimeZone.UTC );
        final MapPokemonOuterClass.MapPokemon cp = catchablePokemons.get(marker);
        if(cp != null){
            long timeTilHidden = timeTillHidden(cp, nowUtc);

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
                                removeMarkerAnimated(marker);
                                catchablePokemons.remove(marker);
                                if(pokemonCache != null){
                                    pokemonCache.put(String.valueOf(cp.getEncounterId()), cp.getSpawnPointId());
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
                            removeMarkerAnimated(marker);
                            catchablePokemons.remove(marker);
                        }catch (Exception e){}
                    }
                }, 1000);
            }
            return false;

        }

        final WildPokemonOuterClass.WildPokemon pokemon = pokemons.get(marker);
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
                                removeMarkerAnimated(marker);
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
                            removeMarkerAnimated(marker);
                            pokemons.remove(marker);
                        }catch (Exception e){}
                    }
                }, 1000);
            }
            return false;

        }


        final Pokestop pokestop = pokestops.get(marker);
        if(pokestop != null){
            if("pokestop".equals(marker.getTitle())){
                AsyncTask at = new AsyncTask() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        marker.setTitle(getString(R.string.loading_));
                        marker.hideInfoWindow();
                        marker.showInfoWindow();
                    }

                    @Override
                    protected Object doInBackground(Object[] params) {
                        try {
                            return pokestop.getDetails().getName();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        if(o instanceof  String){
                            marker.setTitle((String)o);
                            marker.hideInfoWindow();
                            marker.showInfoWindow();
                        }
                    }
                };
                at.execute();
            }

            Duration d = new Duration(timeTilX(pokestop.getCooldownCompleteTimestampMs(), nowUtc));
            if(d.getMillis() > 0){
                make(mView,
                        MessageFormat.format(
                                getString(R.string.pokestop_info),
                                d.getStandardMinutes(),
                                d.getStandardSeconds() - 60*d.getStandardMinutes()
                        ), Snackbar.LENGTH_LONG)
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

    public void removeMarkerAnimated(final Marker marker){
        animateMarker(marker, true);
    }
    public void addMarkerAnimated(final Marker marker){
        animateMarker(marker, false);
    }

    @MainThread
    private void animateMarker(final Marker marker, final boolean remove) {
        Property<Marker, Float> property = Property.of(Marker.class, Float.class, "alpha");
        ObjectAnimator animator = ObjectAnimator.ofFloat(marker, property,
                remove?1f:0f, remove?0f:1f);
        animator.setDuration(400);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if(remove){
                    marker.remove();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                if(remove){
                    marker.remove();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }


}

