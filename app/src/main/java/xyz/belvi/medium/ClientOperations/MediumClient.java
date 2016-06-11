package xyz.belvi.medium.ClientOperations;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import xyz.belvi.medium.Callback.ConnectionCallback;
import xyz.belvi.medium.Callback.MediumConnectionCallback;
import xyz.belvi.medium.Callback.MediumUserAuthCallback;
import xyz.belvi.medium.Enums.ApiHost;
import xyz.belvi.medium.Enums.ErrorCodes;
import xyz.belvi.medium.Enums.Scope;
import xyz.belvi.medium.MediumObject.MediumError;
import xyz.belvi.medium.MediumObject.MediumImage;
import xyz.belvi.medium.MediumObject.MediumUser;
import xyz.belvi.medium.MediumObject.OauthDetails;
import xyz.belvi.medium.MediumObject.Post;
import xyz.belvi.medium.Network.NetworkCall;
import xyz.belvi.medium.Network.NetworkResponse;
import xyz.belvi.medium.RequestParams.RequestParams;
import xyz.belvi.medium.RequestParams.ResponseType;

/**
 * Created by zone2 on 6/10/16.
 */
public class MediumClient {

    private String url;
    private static Activity mActivity;

    private static OauthDetails oauthDetails;
    private static ApiHost apiHost;
    private static String networkParams;

    private static ConnectionCallback connectionCallback;

    public MediumClient() {
    }

    private MediumClient(String url) {
        this.url = url;

    }

    public void connect() {
        if (apiHost == ApiHost.REQUEST_CODE) {
            mActivity.startActivity(new Intent(mActivity, AuthHandler.class).putExtra(AuthHandler.URL, url));
        } else {
            new AsyncTask<Void, Void, NetworkResponse>() {

                @Override
                protected NetworkResponse doInBackground(Void... params) {
                    return NetworkCall.connect(url, apiHost, networkParams, oauthDetails);
                }

                @Override
                protected void onPostExecute(NetworkResponse networkResponse) {
                    super.onPostExecute(networkResponse);
                    if (networkResponse.isSuccess()) {
                        switch (apiHost) {
                            case ACCESS_TOKEN:
                                ((MediumConnectionCallback) connectionCallback).onAccessTokenRetrieved(new OauthDetails(networkResponse.getResponseString()));
                                break;
                            case REFRESH_TOKEN:
                                ((MediumConnectionCallback) connectionCallback).onAccessTokenRefreshed(new OauthDetails(networkResponse.getResponseString()));
                                break;
                            case ME:
                                ((MediumUserAuthCallback) connectionCallback).onUserDetailsRetrieved(new MediumUser(networkResponse.getResponseString()));

                        }
                    } else {

                        MediumError mediumError = new MediumError(networkResponse.getResponseString());
                        connectionCallback.connectionFailed(mediumError);

                    }
                }
            }.execute();
        }
    }


    public static final class Builder {
        private HashSet<Scope> requestScope;
        private Post post;
        private MediumImage mediumImage;
        private HashMap<String, String> params;
        private String userId, state;
        private String publicationId;

        public Builder(Activity activity, ApiHost apiHost) {
            mActivity = activity;
            MediumClient.apiHost = apiHost;
            requestScope = new HashSet<>();
            oauthDetails = new OauthDetails();
            params = new HashMap<>();

        }

        private String getParamsData() throws UnsupportedEncodingException {
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey().toString(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            return postData.toString();
        }

        public MediumClient build() throws UnsupportedEncodingException {

            if (apiHost == ApiHost.REQUEST_CODE) {
                LocalBroadcastManager.getInstance(mActivity).registerReceiver(clientConnectionReceiver, new IntentFilter(ClientConstant.connectionReceiverAction));
                params.put(RequestParams.RESPONSE_TYPE, ResponseType.CODE);
                String apiScope = "";
                for (Scope scope : requestScope) {
                    apiScope += scope.getScopeName() + ",";
                }
                if (apiScope.endsWith(",")) {
                    apiScope = apiScope.substring(0, apiScope.length() - 1);
                }
                params.put(RequestParams.SCOPE, apiScope);

                String url = apiHost.getUriPath() + getParamsData();
                return new MediumClient(url);
            } else if (apiHost == ApiHost.ACCESS_TOKEN || apiHost == ApiHost.REFRESH_TOKEN) {
                params.put(RequestParams.GRANT_TYPE, apiHost.getGrantType());
                networkParams = getParamsData();
                return new MediumClient(apiHost.getUriPath());

            } else {
                return new MediumClient(apiHost.getUriPath());
            }
        }

        BroadcastReceiver clientConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isAuthSuccessful = intent.getBooleanExtra(ClientConstant.connectionStatus, false);
                if (isAuthSuccessful) {
                    boolean grantedAccess = intent.getBooleanExtra(ClientConstant.connectionAccessStatus, false);
                    if (grantedAccess) {
                        ((MediumConnectionCallback) connectionCallback).onCodeRetrieved(intent.getExtras());
                    } else {
                        ((MediumConnectionCallback) connectionCallback).onAccessDenied();
                    }

                } else
                    connectionCallback.connectionFailed(new MediumError("Error occured when making request", ErrorCodes.CONNECTION_FAILED.getErrorCode()));

                LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(clientConnectionReceiver);
            }
        };

        public Builder changeHost(ApiHost apiHost) {
            MediumClient.apiHost = apiHost;
            return this;
        }

        public Builder addScope(Scope scope) {
            requestScope.add(scope);
            return this;
        }

        public Builder clientID(String clientId) {
            params.put(RequestParams.CLIENT_ID, clientId);
            return this;
        }

        public Builder redirectUri(String redirect_uri) {
            params.put(RequestParams.REDIRECT_URI, redirect_uri == null ? RequestParams.UNOFFICIAL_URI : redirect_uri.trim().isEmpty() ? RequestParams.UNOFFICIAL_URI : redirect_uri);
            return this;
        }

        public Builder addConnectionCallback(ConnectionCallback connectionCallback) {
            MediumClient.connectionCallback = connectionCallback;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            params.put(RequestParams.CLIENT_SECRET, clientSecret);
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            params.put(RequestParams.REFRESH_TOKEN, refreshToken);
            return this;
        }

        public Builder accessToken(String accessToken) {
            oauthDetails.setAccessToken(accessToken);
            return this;
        }

        public Builder tokenType(String tokenType) {
            oauthDetails.setTokenType(tokenType);
            return this;
        }

        public Builder addImage(MediumImage mediumImage) {
            this.mediumImage = mediumImage;
            return this;
        }

        public Builder publish(Post post) {
            this.post = post;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder state(String state) {
            params.put(RequestParams.STATE, state);
            return this;
        }

        public Builder code(String code) {
            params.put(RequestParams.CODE, code);
            return this;
        }

        public Builder publicationId(String publicationId) {
            this.publicationId = publicationId;
            return this;
        }


    }
}