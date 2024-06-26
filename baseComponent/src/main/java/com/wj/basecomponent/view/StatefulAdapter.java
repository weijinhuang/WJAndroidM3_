package com.wj.basecomponent.view;

import android.os.Parcelable;

import androidx.annotation.NonNull;

public interface StatefulAdapter {
    /**
     * Saves adapter state
     */
    @NonNull
    Parcelable saveState();

    /**
     * Restores adapter state
     */
    void restoreState(@NonNull Parcelable savedState);
}