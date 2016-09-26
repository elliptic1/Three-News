package com.tbse.threenews;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

/**
 * Created by todd on 9/25/16 for the Android Nanodegree capstone project.
 */

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setPadding(300, 100, 300, 100);
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.peter_river));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

}
