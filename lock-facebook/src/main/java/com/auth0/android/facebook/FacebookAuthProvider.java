package com.auth0.android.facebook;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.provider.AuthProvider;
import com.auth0.android.result.Credentials;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;

import java.util.Collection;
import java.util.Collections;

/**
 * Native Facebook Sign In implementation of the Auth0 AuthProvider.
 */
public class FacebookAuthProvider extends AuthProvider {

    private static final String TAG = FacebookAuthProvider.class.getSimpleName();
    private final AuthenticationAPIClient auth0Client;

    private Collection<String> permissions;
    private String connectionName;
    private FacebookApiHelper apiHelper;

    /**
     * @param client an Auth0 AuthenticationAPIClient instance
     */
    public FacebookAuthProvider(@NonNull AuthenticationAPIClient client) {
        this.auth0Client = client;
        this.permissions = Collections.singleton("public_profile");
        this.connectionName = "facebook";
    }

    /**
     * Change the scope to request on the user login. Use any of the permissions defined in https://developers.facebook.com/docs/facebook-login/android/permissions. Must be called before start().
     * The permission "public_profile" is requested by default.
     *
     * @param permissions the permissions to add to the request
     */
    public void setPermissions(@NonNull Collection<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Change the default connection to use when requesting the token to Auth0 server. By default this value is "facebook".
     *
     * @param connection that will be used to authenticate the user against Auth0.
     */
    public void setConnection(@NonNull String connection) {
        this.connectionName = connection;
    }

    @Override
    protected void requestAuth(Activity activity, int requestCode) {
        apiHelper = createApiHelper(activity);
        apiHelper.login(activity, requestCode, permissions);
    }

    @Override
    public boolean authorize(int requestCode, int resultCode, @Nullable Intent intent) {
        return apiHelper.finishLogin(requestCode, resultCode, intent);
    }

    @Override
    public boolean authorize(@Nullable Intent intent) {
        //Unused
        return false;
    }

    @Override
    public String[] getRequiredAndroidPermissions() {
        return new String[0];
    }

    @Override
    public void stop() {
        super.stop();
        clearSession();
    }

    @Override
    public void clearSession() {
        super.clearSession();
        if (apiHelper != null) {
            apiHelper.logout();
            apiHelper = null;
        }
    }


    Collection<String> getPermissions() {
        return permissions;
    }

    String getConnection() {
        return connectionName;
    }

    FacebookApiHelper createApiHelper(Activity activity) {
        return new FacebookApiHelper(createFacebookCallback());
    }

    FacebookCallback<LoginResult> createFacebookCallback() {
        return new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (loginResult.getRecentlyDeniedPermissions().isEmpty()) {
                    requestAuth0Token(loginResult.getAccessToken().getToken());
                } else {
                    Log.w(TAG, "Some permissions were not granted: " + loginResult.getRecentlyDeniedPermissions().toString());
                    callback.onFailure(R.string.com_auth0_facebook_authentication_failed_title, R.string.com_auth0_facebook_authentication_failed_missing_permissions_message, null);
                }
            }

            @Override
            public void onCancel() {
                Log.w(TAG, "User cancelled the log in dialog");
                callback.onFailure(R.string.com_auth0_facebook_authentication_failed_title, R.string.com_auth0_facebook_authentication_cancelled_error_message, null);
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "Error on log in: " + error.getMessage());
                callback.onFailure(R.string.com_auth0_facebook_authentication_failed_title, R.string.com_auth0_facebook_authentication_failed_message, error);
            }
        };
    }

    private void requestAuth0Token(String token) {
        auth0Client.loginWithOAuthAccessToken(token, connectionName)
                .start(new AuthenticationCallback<Credentials>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        callback.onSuccess(credentials);
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        callback.onFailure(R.string.com_auth0_facebook_authentication_failed_title, R.string.com_auth0_facebook_authentication_failed_message, error);
                    }
                });
    }
}
