package org.mariotaku.pass.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.mariotaku.pass.R;
import org.mariotaku.pass.view.RememberMasterPasswordContainer;

/**
 * Created by mariotaku on 15/10/31.
 */
public class RememberMasterPasswordPatternFragment extends Fragment implements View.OnClickListener, RememberMasterPasswordContainer.PageListener {
    private RememberMasterPasswordContainer mViewPages;
    private Button mPreviousButton, mNextButton;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mNextButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mViewPages.setPageListener(this);

        updatePage();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remember_master_password_pattern, container, false);
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
