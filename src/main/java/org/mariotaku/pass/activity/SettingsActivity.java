package org.mariotaku.pass.activity;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.fragment.SettingsDetailsFragment;

/**
 * Created by mariotaku on 15/10/31.
 */
public class SettingsActivity extends PreferenceActivity implements Constants {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_SHOW_FRAGMENT) && !intent.hasExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)) {
            final Bundle fragmentArgs = new Bundle();
            fragmentArgs.putInt(EXTRA_RESID, R.xml.pref_general);
            intent.putExtra(EXTRA_SHOW_FRAGMENT, SettingsDetailsFragment.class.getName());
            intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, fragmentArgs);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        try {
            return Fragment.class.isAssignableFrom(Class.forName(fragmentName));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
