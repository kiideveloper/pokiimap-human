/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zl.pokemap.betterpokemap.auth;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.apptopus.progressive.Progressive;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.pokegoapi.exceptions.LoginFailedException;
import com.zl.pokemap.betterpokemap.PokiiMapApplication;
import com.zl.pokemap.betterpokemap.R;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;

public class AuthUiActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    @BindView(R.id.google)
    RadioButton mGoogle;
    @BindView(R.id.ptc)
    RadioButton mPtc;
    @BindView(R.id.username)
    EditText mUsername;
    @BindView(R.id.password)
    EditText mPassword;


    @BindView(R.id.sign_in)
    Button mSignIn;
    @BindView(R.id.form)
    View mForm;

    @BindView(android.R.id.content)
    View mRootView;
    private Tracker mTracker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PokiiMapApplication application = (PokiiMapApplication) getApplication();
        mTracker = application.getDefaultTracker();
        setContentView(R.layout.auth_ui_layout);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();

        }
        return true;
    }

    @OnClick(R.id.sign_in)
    public void signIn(View view) {
        final Login login = getSelectedLogin();
        final String username = mUsername.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();

        AsyncTask at = new AsyncTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Progressive.showProgress(mForm);
                mSignIn.setEnabled(false);
                mUsername.setEnabled(false);
                mPassword.setEnabled(false);
            }

            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = login.login(username, password);
                    return auth;
                } catch (LoginFailedException e) {
                    e.printStackTrace();;
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                Progressive.hideProgress(mForm);
                mSignIn.setEnabled(true);
                mUsername.setEnabled(true);
                mPassword.setEnabled(true);
                if(o instanceof RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo){
                    RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = (RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo)o;
                    setResult(RESULT_OK);
                    finish();
                }
                Snackbar.make(mRootView, String.valueOf(o), Snackbar.LENGTH_LONG).show();
            }
        };

        at.execute();

    }



    @MainThread
    private Login getSelectedLogin() {
        OkHttpClient client = new OkHttpClient();
        if (mGoogle.isChecked()) {
            return new PmGoogleLogin(client, this);
        }
        return new PmPTCLogin(client, this);
    }


    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    public static Intent createIntent(Context context) {
        Intent in = new Intent();
        in.setClass(context, AuthUiActivity.class);
        return in;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Login");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!TextUtils.isEmpty(mUsername.getText())){
            SharedPreferencesCompat.EditorCompat.getInstance().apply(
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("last_username", String.valueOf(mUsername.getText())));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(TextUtils.isEmpty(mUsername.getText())){
            String lastUserName = PreferenceManager.getDefaultSharedPreferences(this).getString("last_username", "");
            if(!TextUtils.isEmpty(lastUserName)){
                mUsername.setText(lastUserName);
                mPassword.requestFocus();
            }
        }
    }
}
