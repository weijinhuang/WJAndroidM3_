package com.wj.basecomponent.view.viewpager;

import androidx.annotation.NonNull;
import androidx.annotation.Px;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class CompositeOnPageChangeCallback extends LongSeViewPager.OnPageChangeCallback {
    @NonNull
    private final List<LongSeViewPager.OnPageChangeCallback> mCallbacks;

    CompositeOnPageChangeCallback(int initialCapacity) {
        mCallbacks = new ArrayList<>(initialCapacity);
    }

    /**
     * Adds the given callback to the list of subscribers
     */
    void addOnPageChangeCallback(LongSeViewPager.OnPageChangeCallback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Removes the given callback from the list of subscribers
     */
    void removeOnPageChangeCallback(LongSeViewPager.OnPageChangeCallback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * @see LongSeViewPager.OnPageChangeCallback#onPageScrolled(int, float, int)
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels) {
        try {
            for (LongSeViewPager.OnPageChangeCallback callback : mCallbacks) {
                callback.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        } catch (ConcurrentModificationException ex) {
            throwCallbackListModifiedWhileInUse(ex);
        }
    }

    /**
     * @see LongSeViewPager.OnPageChangeCallback#onPageSelected(int)
     */
    @Override
    public void onPageSelected(int position) {
        try {
            for (LongSeViewPager.OnPageChangeCallback callback : mCallbacks) {
                callback.onPageSelected(position);
            }
        } catch (ConcurrentModificationException ex) {
            throwCallbackListModifiedWhileInUse(ex);
        }
    }

    /**
     * @see LongSeViewPager.OnPageChangeCallback#onPageScrollStateChanged(int)
     */
    @Override
    public void onPageScrollStateChanged(@LongSeViewPager.ScrollState int state) {
        try {
            for (LongSeViewPager.OnPageChangeCallback callback : mCallbacks) {
                callback.onPageScrollStateChanged(state);
            }
        } catch (ConcurrentModificationException ex) {
            throwCallbackListModifiedWhileInUse(ex);
        }
    }

    private void throwCallbackListModifiedWhileInUse(ConcurrentModificationException parent) {
        throw new IllegalStateException(
                "Adding and removing callbacks during dispatch to callbacks is not supported",
                parent
        );
    }

}
