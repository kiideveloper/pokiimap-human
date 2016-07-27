package com.zl.pokemap.betterpokemap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.function.Function;
import com.apptopus.progressive.Progressive;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.player.PlayerProfile;
import com.zl.pokemap.betterpokemap.auth.AuthUiActivity;
import com.zl.pokemap.betterpokemap.auth.CredentialProviderAdapter;
import com.zl.pokemap.betterpokemap.auth.PmGoogleLogin;
import com.zl.pokemap.betterpokemap.auth.PmPTCLogin;
import com.zl.pokemap.betterpokemap.map.MapWrapperFragment;
import com.zl.pokemap.betterpokemap.settings.SettingsActivity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import okhttp3.OkHttpClient;

import static com.zl.pokemap.betterpokemap.R.id.headline;
import static com.zl.pokemap.betterpokemap.R.id.username;

public class PokeMapsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MapWrapperFragment.LocationRequestListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String TAG = "Pokemap";

    // fragments
    private MapWrapperFragment mMapWrapperFragment;

    // Google api shit
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    private ProgressBar progress;
    private TextView mStatus;
    // Preferences
    private SharedPreferences pref;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ExecutorService executors = Executors.newSingleThreadExecutor();

    View mRootView;

    Button mSignin;
    View mSignout;

    TextView mHeadLine;
    TextView mUsername;
    TextView mTeam;
    TextView mStats;

    private OkHttpClient http = new OkHttpClient();
    private PokemonGo pokemonGo;
    private Tracker mTracker;

    public PokemonGo getPokemonGo() {
        return pokemonGo;
    }

    @MainThread
    public void showProgress(){
        mStatus.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
    }

    @MainThread
    public void hideProgress(){
        mStatus.postDelayed(new Runnable() {
            @Override
            public void run() {
                mStatus.setVisibility(View.GONE);
            }
        }, 2000);
        progress.setVisibility(View.INVISIBLE);
    }

    @MainThread
    public void setStatus(String status){
        mStatus.setText(status);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PokiiMapApplication application = (PokiiMapApplication) getApplication();
        mTracker = application.getDefaultTracker();

        setContentView(R.layout.activity_main);


        mRootView = findViewById(R.id.main_container);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.zzz_menu,  /* nav drawer icon to replace 'Up' caret */
                R.string.app_name,  /* "open drawer" description */
                R.string.user_profile_header  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(getString(R.string.app_name));
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(R.string.profile);
//                updateProfile();
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.zzz_menu);


        pref = PreferenceManager.getDefaultSharedPreferences(this);
        //drawer
        NavigationView navigationView = (NavigationView) findViewById(R.id.left_drawer);
        View headerLayout = navigationView.inflateHeaderView(R.layout.signed_in_layout);

        mSignin = (Button) headerLayout.findViewById(R.id.sign_in);
        mSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        mSignout = headerLayout.findViewById(R.id.sign_out);
        mSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().remove("auth").remove("username").remove("token").remove("expiry").remove("auth_provider").commit();
            }
        });
        mHeadLine = (TextView) headerLayout.findViewById(headline);
        mUsername = (TextView) headerLayout.findViewById(username);
        mTeam = (TextView) headerLayout.findViewById(R.id.team);
        mStats = (TextView) headerLayout.findViewById(R.id.stats);
        //drawer

        mStatus = (TextView) findViewById(R.id.status);
        progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.INVISIBLE);

        FloatingActionButton locationFab = (FloatingActionButton) findViewById(R.id.location_fab);
        locationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String authJson = pref.getString("auth", "");
                if(TextUtils.isEmpty(authJson)){
                    askLogin();
                    return;
                }
                if(progress.getVisibility() != View.VISIBLE && shouldRefreshToken()){
                    showProgress();
                    executors.submit(new Runnable() {
                        @Override
                        public void run() {
                            refreshToken();
                        }
                    });
                    executors.submit(new Runnable() {
                        @Override
                        public void run() {
                            if(mMapWrapperFragment!= null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mMapWrapperFragment.showPokemon();
                                    }
                                });
                            }
                        }
                    });
                }else if(mMapWrapperFragment!= null){
                    if(pokemonGo == null){
                        askLogin();
                    }else{
                        mMapWrapperFragment.showPokemon();
                    }
                }
            }
        });

//        locationFab.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
                    //do something awesome
//                return true;
//            }
//        });


        setUpGoogleApiClient();

        mMapWrapperFragment = MapWrapperFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_container, mMapWrapperFragment)
                .commit();

        initPokemonGOApi(new ProfileUpdateFunction());

    }

    @MainThread
    public void askLogin(){
        setProfile("", "", Collections.<String, String>emptyMap());
        if(mDrawerLayout != null && !mDrawerLayout.isDrawerOpen(Gravity.LEFT)){
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }
    @WorkerThread
    public void askLoginWorker(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                askLogin();
            }
        });
    }

    @MainThread
    private void setProfile(String username, String team, Map<String, String> stats){
        if(TextUtils.isEmpty(username)){
            mSignout.setVisibility(View.GONE);
            mSignin.setVisibility(View.VISIBLE);
            mHeadLine.setText(R.string.start_header);
        }else{
            mSignout.setVisibility(View.VISIBLE);
            mSignin.setVisibility(View.GONE);
            mHeadLine.setText(R.string.signed_in_header);
        }

        mUsername.setText(username);
        mTeam.setText(team);
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> e : stats.entrySet()){
            sb.append(e.getKey()+": "+e.getValue()).append("<br/>");
        }
        mStats.setText(Html.fromHtml(sb.toString()));

    }

    @MainThread
    public void showMessage(String e){
        showMessage(e, Snackbar.LENGTH_SHORT);

    }

    @MainThread
    public void showMessage(String e, int length){
        if(mRootView != null){
            Snackbar.make(mRootView, e, length).show();
        }

    }

    @MainThread
    public void showMessage(@StringRes int s, int length){
        if(mRootView != null){
            Snackbar.make(mRootView, s, length).show();
        }

    }

    @MainThread
    public void showMessage(@StringRes int s){
        if(mRootView != null){
            Snackbar.make(mRootView, s, Snackbar.LENGTH_SHORT).show();
        }

    }

    private boolean shouldRefreshToken(){
        final String username = pref.getString("username", "");
        final String accessToken = pref.getString("token", "");
        final String provider = pref.getString("auth_provider", "");
        long expiry = pref.getLong("expiry", -1);
        if(expiry < 0 || System.currentTimeMillis() < expiry
                || TextUtils.isEmpty(username)
                || TextUtils.isEmpty(accessToken)
                || TextUtils.isEmpty(provider)){
            //nothing to do, user will need to login agian
            return false;
        }
        return true;
    }

    @WorkerThread
    private void refreshToken(){
        final String username = pref.getString("username", "");
        final String accessToken = pref.getString("token", "");
        final String provider = pref.getString("auth_provider", "");

        try {
            if("ptc".equals(provider)){

                final PmPTCLogin login = new PmPTCLogin(http, PokeMapsActivity.this);
                login.login(login.oauth(username, accessToken));

            }else{
                final PmGoogleLogin login = new PmGoogleLogin(http, PokeMapsActivity.this);
                login.login(login.oauth(username, accessToken));

            }
        }catch (Exception e){
            e.printStackTrace();
            pref.edit().remove("expiry").commit();
        }

    }


    private void initPokemonGOApi(final Function<Object, Object> callback){
        final String authJson = pref.getString("auth", "");

        if(TextUtils.isEmpty(authJson)){
            askLogin();
            return;
        }

        AsyncTask at = new AsyncTask() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
//                mSignin.setEnabled(false);
                mSignin.setText(R.string.loading_);
            }

            @Override
            protected Object doInBackground(Object[] objects) {

                try {
                    if(!TextUtils.isEmpty(authJson)){
                        Gson gson = new Gson();

                        final RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth =
                                gson.fromJson(authJson, RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo.class);
                        pokemonGo = new PokemonGo(new CredentialProviderAdapter(auth), http);
                        PlayerProfile profile = pokemonGo.getPlayerProfile();
                        return profile;
                    }
                }catch (Exception e){
                    e.printStackTrace();
//                    final String error = e.getMessage();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            showMessage(error);
//                        }
//                    });
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
//                mSignin.setEnabled(false);
                mSignin.setText(R.string.sign_in);
                if(o instanceof  PlayerProfile){
                    PlayerProfile playerProfile = (PlayerProfile)o;
                    Map<String, String> stats = new HashMap<>();
                    stats.put("Level", String.valueOf(playerProfile.getStats().getLevel()));
                    stats.put("XP", String.valueOf(playerProfile.getStats().getExperience()));

                    setProfile(playerProfile.getUsername(), playerProfile.getTeam().name(), stats);
                }else{
                    setProfile("", "", Collections.<String, String>emptyMap());
                }

                if(callback != null){
                    callback.apply(o);
                }

            }
        };
        at.execute();
    }

    @MainThread
    private void login() {
        final String authJson = pref.getString("auth", "");

        if(!TextUtils.isEmpty(authJson)){
            initPokemonGOApi(new ProfileUpdateFunction(){
                @Override
                public Object apply(Object o) {
                    if(o instanceof PlayerProfile){
                        return super.apply(o);
                    }else{
                        startActivityForResult(new Intent(PokeMapsActivity.this, AuthUiActivity.class), 1);
                    }
                    return Void.TYPE;
                }
            });
            return;
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1 && resultCode == RESULT_OK){
            if(mDrawerLayout != null){
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            }
            showMessage(R.string.loggedin_message,
                    Snackbar.LENGTH_LONG);
            initPokemonGOApi(new ProfileUpdateFunction());
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setUpGoogleApiClient() {
        if (mGoogleApiClient == null) mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }


        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        pref.registerOnSharedPreferenceChangeListener(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        pref.unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /// GOOGLE FUSED LOCATION API CALLBACKS ///

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            mMapWrapperFragment.onLocationReady(mLastLocation);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /// INTERFACES ///

    @Override
    public Location requestLocation() {
        if (mLastLocation == null) {
            setUpGoogleApiClient();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        return mLastLocation;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if("auth".equals(key)){
            initPokemonGOApi(new ProfileUpdateFunction());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("PokiiMap");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private class ProfileUpdateFunction implements Function<Object, Object>{

        @Override
        @MainThread
        public Object apply(Object o) {
            if(o instanceof  PlayerProfile){
                PlayerProfile playerProfile = (PlayerProfile)o;
                Map<String, String> stats = new HashMap<>();
                stats.put("Level", String.valueOf(playerProfile.getStats().getLevel()));
                stats.put("XP", String.valueOf(playerProfile.getStats().getExperience()));
                setProfile(playerProfile.getUsername(), playerProfile.getTeam().name(), stats);
            }else if(o != null){
                showMessage(String.valueOf(o));
            }
            return Void.TYPE;
        }
    }

    private AtomicBoolean isUpdatingProfile = new AtomicBoolean(false);
    @MainThread
    public void updateProfile(){
        if(isUpdatingProfile.get()){
            return;
        }
        try {
            final NavigationView navigationView = (NavigationView) findViewById(R.id.left_drawer);
            if(pokemonGo!= null){
                AsyncTask at = new AsyncTask() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        if(navigationView != null){
                            Progressive.showProgress(navigationView);
                        }
                        isUpdatingProfile.set(true);
                    }

                    @Override
                    protected Object doInBackground(Object[] params) {
                        try {
                            PlayerProfile profile = pokemonGo.getPlayerProfile();
                            profile.updateProfile();
                            return profile;
                        } catch (Exception e) {
                            return e;
                        }
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        isUpdatingProfile.set(false);
                        if(navigationView != null){
                            Progressive.hideProgress(navigationView);
                        }
                        new ProfileUpdateFunction().apply(o);
                    }
                };
                at.execute();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
