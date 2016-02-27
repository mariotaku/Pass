package org.mariotaku.pass.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.mariotaku.pass.BuildConfig;
import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.provider.PassProvider.PassDataStore.NfcTags;
import org.mariotaku.sqliteqb.library.Expression;

import okio.ByteString;

/**
 * Created by mariotaku on 15/11/17.
 */
public class NfcTagsListFragment extends ListFragment implements Constants, LoaderManager.LoaderCallbacks<Cursor> {

    private BroadcastReceiver mNfcReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            final byte[] id = tag.getId();
            if (id.length > 0) {
                final String where = Expression.equalsArgs(NfcTags.TAG_ID).getSQL();
                final String[] whereArgs = {ByteString.of(id).hex()};
                try (Cursor cursor = getActivity().getContentResolver().query(NfcTags.CONTENT_URI,
                        NfcTags.COLUMNS, where, whereArgs, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                    } else {
                        showAddTagDialog(tag);
                    }
                }
            } else {
                Toast.makeText(getActivity(), R.string.unsupported_tag, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void showAddTagDialog(final Tag tag) {
        final AddCardDialogFragment df = new AddCardDialogFragment();
        final Bundle args = new Bundle();
        args.putParcelable(NfcAdapter.EXTRA_TAG, tag);
        df.setArguments(args);
        df.show(getFragmentManager(), "add_card");
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setListAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_activated_2, null,
                new String[]{NfcTags.NAME, NfcTags.DOMAIN},
                new int[]{android.R.id.text1, android.R.id.text2}, 0));
        getLoaderManager().initLoader(0, null, this);
        setListShownNoAnimation(false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(getActivity(), NfcTags.CONTENT_URI, NfcTags.COLUMNS, null, null, null);
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

    @Override
    public void onPause() {
        super.onPause();
        final Activity activity = getActivity();
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.disableForegroundDispatch(activity);
        }
        activity.unregisterReceiver(mNfcReceiver);
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        activity.registerReceiver(mNfcReceiver, new IntentFilter(ACTION_TAG_DISCOVERED));
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            final Intent intent = new Intent(ACTION_TAG_DISCOVERED);
            intent.setPackage(BuildConfig.APPLICATION_ID);
            nfcAdapter.enableForegroundDispatch(activity, PendingIntent.getBroadcast(activity,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT), null, null);
        }
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_tags_list, menu);
    }

    public static class AddCardDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener, TextWatcher {
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.add_card);
            builder.setView(R.layout.dialog_add_card);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(android.R.string.ok, this);
            final AlertDialog dialog = builder.create();
            dialog.setOnShowListener(this);
            return dialog;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    final ContentValues values = new ContentValues();
                    final AlertDialog alertDialog = (AlertDialog) getDialog();
                    final EditText editHost = (EditText) alertDialog.findViewById(R.id.edit_host);
                    final EditText editName = (EditText) alertDialog.findViewById(R.id.edit_name);
                    final Editable editHostText = editHost.getText(), editNameText = editName.getText();
                    if (TextUtils.isEmpty(editHostText)) return;
                    values.put(NfcTags.DOMAIN, editHostText.toString());
                    values.put(NfcTags.NAME, editNameText.toString());
                    values.put(NfcTags.TAG_ID, ByteString.of(getNfcTag().getId()).hex());
                    getActivity().getContentResolver().insert(NfcTags.CONTENT_URI, values);
                    break;
                }
            }
        }

        public Tag getNfcTag() {
            return getArguments().getParcelable(NfcAdapter.EXTRA_TAG);
        }

        @Override
        public void onShow(final DialogInterface dialog) {
            final AlertDialog alertDialog = (AlertDialog) dialog;
            final EditText editHost = (EditText) alertDialog.findViewById(R.id.edit_host);
            final EditText editName = (EditText) alertDialog.findViewById(R.id.edit_name);
            final byte[] id = getNfcTag().getId();
            final ByteString bs = ByteString.of(id);
            final String idHex = bs.hex();
            if (idHex.length() > 8) {
                editName.setText(getString(R.string.tag_name_prefix, idHex.substring(idHex.length() - 8)));
            } else {
                editName.setText(getString(R.string.tag_name_prefix, idHex));
            }
            editHost.addTextChangedListener(this);

            updatePositiveButton();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

        }

        @Override
        public void afterTextChanged(final Editable s) {
            updatePositiveButton();
        }

        private void updatePositiveButton() {
            final AlertDialog alertDialog = (AlertDialog) getDialog();
            final Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            final EditText editHost = (EditText) alertDialog.findViewById(R.id.edit_host);
            final EditText editName = (EditText) alertDialog.findViewById(R.id.edit_name);
            button.setEnabled(editHost.length() > 0);
        }
    }
}
