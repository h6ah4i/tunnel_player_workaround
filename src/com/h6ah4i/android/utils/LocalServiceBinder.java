
package com.h6ah4i.android.utils;

import java.lang.ref.WeakReference;

import android.os.Binder;

public class LocalServiceBinder<T> extends Binder {
    @SuppressWarnings("unused")
    private static final String TAG = "LocalServiceBinder";
    private final WeakReference<T> mService;

    public LocalServiceBinder(T service) {
        mService = new WeakReference<T>(service);
    }

    public T getService() {
        return mService.get();
    }
}
