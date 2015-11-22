package org.mariotaku.pass.fragment;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.provider.PassProvider.PassDataStore.AppWindows;

/**
 * Created by mariotaku on 15/11/17.
 */
public class AppWindowsListFragment extends ListFragment implements Constants, LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_activated_2, null,
                new String[]{AppWindows.WINDOW_NAME, AppWindows.DOMAIN},
                new int[]{android.R.id.text1, android.R.id.text2}, 0));
        getLoaderManager().initLoader(0, null, this);
        setListShownNoAnimation(false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(getActivity(), AppWindows.CONTENT_URI, AppWindows.COLUMNS, null, null, null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        ((SimpleCursorAdapter) getListAdapter()).changeCursor(data);
        setListShown(true);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
    }

}
