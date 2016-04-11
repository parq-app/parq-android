package com.mmm.parq.utils;

import android.app.Activity;
import android.content.Context;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.regions.Regions;
import com.firebase.client.Firebase;
import com.mmm.parq.R;

public class AwsDeveloperAuthenticationProvider extends AWSAbstractCognitoDeveloperIdentityProvider {
    private static String mDeveloperProvider;
    private Firebase mFirebaseRef;

    public AwsDeveloperAuthenticationProvider(Context activity, String accountId, String identityPoolId, Regions region) {
        super(accountId, identityPoolId, region);
        // Initialize any other objects needed here.
        mDeveloperProvider = activity.getString(R.string.developer_provider_name);
        mFirebaseRef = new Firebase(activity.getString(R.string.firebase_endpoint));
    }

    // Return the developer provider name which you choose while setting up the
    // identity pool in the &COG; Console

    @Override
    public String getProviderName() {
        return mDeveloperProvider;
    }

    // Use the refresh method to communicate with your backend to get an
    // identityId and token.

    @Override
    public String refresh() {

        // Override the existing token
        setToken(null);

        // Get the identityId and token by making a call to your backend
        // (Call to your backend)

        // Call the update method with updated identityId and token to make sure
        // these are ready to be used from Credentials Provider.

        update(identityId, token);
        return token;

    }

    // If the app has a valid identityId return it, otherwise get a valid
    // identityId from your backend.

    @Override
    public String getIdentityId() {

        // Load the identityId from the cache
//        identityId = cachedIdentityId;

        if (identityId == null) {
            // Call to your backend
            identityId = mFirebaseRef.getAuth().getUid();
        }
        return identityId;
    }
}
