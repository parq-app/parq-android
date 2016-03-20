package com.mmm.parq.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.mmm.parq.R;
import com.mmm.parq.fragments.LoginFragment;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Display the login fragment.
        FragmentManager fragmentManager = getSupportFragmentManager();
        LoginFragment loginFragment = new LoginFragment();
        fragmentManager.beginTransaction().replace(R.id.login_fragment_container, loginFragment).
                commit();
    }
}
