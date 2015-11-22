package org.mariotaku.pass.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import org.mariotaku.pass.R;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by mariotaku on 15/10/30.
 */
public class RememberMasterPasswordContainer extends ViewAnimator {

    private final Animation mSlideOutRightAnimation;
    private final Animation mSlideInRightAnimation;
    private final Animation mSlideOutLeftAnimation;
    private final Animation mSlideInLeftAnimation;

    private PageListener mPageListener;

    public RememberMasterPasswordContainer(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mSlideInLeftAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_left);
        mSlideOutRightAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_right);
        mSlideInRightAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
        mSlideOutLeftAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_left);
    }

    public RememberMasterPasswordContainer(final Context context) {
        this(context, null);
    }

    @Override
    protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(final AttributeSet attrs) {
        return new LayoutParams(this, getContext(), attrs);
    }

    @Override
    public void showNext() {
        final LayoutParams lpCurrent = (LayoutParams) getCurrentView().getLayoutParams();
        if (lpCurrent.getPageController().onPageNextExit()) {
            if (getDisplayedChild() < getChildCount() - 1) {
                setInAnimation(mSlideInRightAnimation);
                setOutAnimation(mSlideOutLeftAnimation);
                super.showNext();
                if (mPageListener != null) {
                    mPageListener.onPageChanged(getDisplayedChild());
                }
            } else {
                if (mPageListener != null) {
                    mPageListener.onReachedEnd();
                }
            }
            final LayoutParams lpNext = (LayoutParams) getCurrentView().getLayoutParams();
            lpNext.getPageController().onPageNextEnter();
        }
    }

    @Override
    public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        ((LayoutParams) params).getPageController().onAttach(child);
    }

    @Override
    public void showPrevious() {
        final LayoutParams lpCurrent = (LayoutParams) getCurrentView().getLayoutParams();
        if (lpCurrent.getPageController().onPagePreviousExit()) {
            if (getDisplayedChild() > 0) {
                setInAnimation(mSlideInLeftAnimation);
                setOutAnimation(mSlideOutRightAnimation);
                super.showPrevious();
                if (mPageListener != null) {
                    mPageListener.onPageChanged(getDisplayedChild());
                }
            } else {
                if (mPageListener != null) {
                    mPageListener.onReachedStart();
                }
            }
            final LayoutParams lpPrevious = (LayoutParams) getCurrentView().getLayoutParams();
            lpPrevious.getPageController().onPagePreviousEnter();
        }
    }

    public PageController findPageControllerById(int id) {
        for (int i = 0, j = getChildCount(); i < j; i++) {
            final View view = getChildAt(i);
            if (id == view.getId()) {
                return ((LayoutParams) view.getLayoutParams()).getPageController();
            }
        }
        return null;
    }

    public PageListener getPageListener() {
        return mPageListener;
    }

    public void setPageListener(final PageListener pageListener) {
        mPageListener = pageListener;
    }

    public interface PageListener {
        void onPageChanged(int current);

        void onReachedEnd();

        void onReachedStart();
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        private final PageController mPageController;

        public LayoutParams(final RememberMasterPasswordContainer container, final Context c, final AttributeSet attrs) {
            super(c, attrs);
            final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.RememberMasterPasswordContainer);
            mPageController = parsePasswordController(a.getString(R.styleable.RememberMasterPasswordContainer_layout_pageController),
                    container, c, attrs);
            a.recycle();
        }

        private static PageController parsePasswordController(final String className, final RememberMasterPasswordContainer container,
                                                              final Context c, final AttributeSet attrs) {
            if (TextUtils.isEmpty(className)) {
                throw new InflateException("You must give PasswordContainer's child a PageController class");
            }
            try {
                return (PageController) Class.forName(className).getConstructor(RememberMasterPasswordContainer.class,
                        Context.class, AttributeSet.class).newInstance(container, c, attrs);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | ClassCastException e) {
                throw new InflateException(e);
            }
        }

        public PageController getPageController() {
            return mPageController;
        }

    }

    public static class PageController {
        private final RememberMasterPasswordContainer container;
        private final Context context;
        private View view;

        protected PageController(RememberMasterPasswordContainer container, Context context, AttributeSet attributeSet) {
            this.container = container;
            this.context = context;
        }

        public RememberMasterPasswordContainer getContainer() {
            return container;
        }

        public Context getContext() {
            return context;
        }

        public View getView() {
            return view;
        }


        protected boolean onPageNextExit() {
            return true;
        }

        protected boolean onPageNextEnter() {
            return true;
        }

        protected boolean onPagePreviousExit() {
            return true;
        }

        protected boolean onPagePreviousEnter() {
            return true;
        }

        protected void onAttach(final View view) {
            this.view = view;
        }
    }
}
