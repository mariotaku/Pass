package org.mariotaku.pass.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.util.FingerprintHelper;
import org.mariotaku.pass.view.RememberMasterPasswordContainer;

/**
 * Created by mariotaku on 15/10/31.
 */
public class RememberMasterPasswordFingerprintFragment extends Fragment implements Constants,
        View.OnClickListener, RememberMasterPasswordContainer.PageListener {
    private RememberMasterPasswordContainer mViewPages;
    private FingerprintHelper mFingerprintHelper;
    private Button mPreviousButton, mNextButton;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFingerprintHelper = FingerprintHelper.getInstance(getActivity());
        mNextButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mViewPages.setPageListener(this);

        final boolean hasPermission = mFingerprintHelper.requestPermission(this, REQUEST_REQUEST_PERMISSION);
        mNextButton.setEnabled(hasPermission);
        if (hasPermission) {
            checkFingerprintSupport();
        }
        updatePage();
    }

    public FingerprintHelper getFingerprintHelper() {
        return mFingerprintHelper;
    }

    @Override

    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_REQUEST_PERMISSION: {
                final boolean hasPermission = mFingerprintHelper.hasPermission();
                mNextButton.setEnabled(hasPermission);
                if (hasPermission) {
                    checkFingerprintSupport();
                }
                break;
            }
        }
    }

    private void checkFingerprintSupport() {
        if (!mFingerprintHelper.isHardwareDetected()) {
            Toast.makeText(getActivity(), R.string.fingerprint_not_supported, Toast.LENGTH_LONG).show();
            getActivity().finish();
        } else if (!mFingerprintHelper.hasEnrolledFingerprints()) {
            Toast.makeText(getActivity(), R.string.no_fingerprint_enrolled, Toast.LENGTH_LONG).show();
            getActivity().finish();
        } else {
            mNextButton.setEnabled(true);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remember_master_password_fingerprint, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewPages = ((RememberMasterPasswordContainer) view.findViewById(R.id.view_pages));
        mPreviousButton = (Button) view.findViewById(R.id.previous);
        mNextButton = (Button) view.findViewById(R.id.next);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.next: {
                mViewPages.showNext();
                break;
            }
            case R.id.previous: {
                mViewPages.showPrevious();
                break;
            }
        }
    }

    @Override
    public void onPageChanged(final int current) {
        updatePage();
    }

    private void updatePage() {
        final int displayedChild = mViewPages.getDisplayedChild();
        if (displayedChild == 0) {
            mPreviousButton.setText(R.string.cancel);
        } else if (displayedChild == mViewPages.getChildCount() - 1) {
            mNextButton.setText(R.string.finish);
        } else {
            mPreviousButton.setText(R.string.previous);
            mNextButton.setText(R.string.next);
        }
    }

    @Override
    public void onReachedEnd() {
        getActivity().finish();
    }

    @Override
    public void onReachedStart() {
        getActivity().finish();
    }
}
