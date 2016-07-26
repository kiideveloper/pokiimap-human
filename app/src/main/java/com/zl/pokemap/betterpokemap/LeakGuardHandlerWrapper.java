package com.zl.pokemap.betterpokemap;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public class LeakGuardHandlerWrapper<T> extends Handler {
    private final WeakReference<T> mOwnerInstanceRef;

    public LeakGuardHandlerWrapper(final T ownerInstance) {
        this(ownerInstance, Looper.myLooper());
    }

    public LeakGuardHandlerWrapper(final T ownerInstance, final Looper looper) {
        super(looper);
        if (ownerInstance == null) {
            throw new NullPointerException("ownerInstance is null");
        }
        mOwnerInstanceRef = new WeakReference<>(ownerInstance);
    }

    public T getOwnerInstance() {
        return mOwnerInstanceRef.get();
    }
}
