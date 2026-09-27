package com.opticast.player.player.mpv;
import android.content.Context;
import android.view.Surface;
/** All methods are called only on the shared mpv worker, never the application looper. */
public final class NativeMpv {
    static { System.loadLibrary("opticast_mpv"); }
    public static native long create(Context context);
    public static native int option(long handle, String key, String value);
    public static native int initialize(long handle);
    public static native int set(long handle, String key, String value);
    public static native String get(long handle, String key);
    public static native int command(long handle, String... args);
    public static native int surface(long handle, Surface surface);
    public static native int[] events(long handle);
    public static native void destroy(long handle);
}
