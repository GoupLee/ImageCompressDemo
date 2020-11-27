package com.gouplee.luban;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * author gouplee
 * date 2020/10/23 10:14
 * remarks
 */

public class Luban implements Handler.Callback {
    private static final String TAG = "Luban";
    private static final String DEFAULT_DISK_CACHE_DIR = "luban_disk_cache";

    private static final int MSG_COMPRESS_SUCCESS = 0;
    private static final int MSG_COMPRESS_START = 1;
    private static final int MSG_COMPRESS_ERROR = 2;

    private List<InputStreamProvider> mStreamProviders;
    private OnCompressListener mCompressListener;
    private String mTargetDir;
    private String mTargetName;
    private int mLeastCompressSize;
    private boolean mFocusAlpha;
    private Handler mHandler;

    private Luban(Builder builder) {
        this.mStreamProviders = builder.mStreamProviders;
        this.mCompressListener = builder.mCompressListener;
        this.mTargetDir = builder.mTargetDir;
        this.mTargetName = builder.mTargetName;
        this.mLeastCompressSize = builder.mLeastCompressSize;
        this.mFocusAlpha = builder.mFocusAlpha;

        // Looper.getMainLooper()这个在我们开发 library 时特别有用，毕竟你不知道别人在调用使用你的库时会在哪个线程初始化，
        // 所以我们在创建 Handler 时每次都通过指定主线程的 Looper 的方式保证库的正常运行。
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    public static Builder with(Context context) {
        return new Builder(context);
    }

    /**
     * start asynchronous compress thread
     * @param context
     */
    private void launch(final Context context) {
        if (mStreamProviders == null || mStreamProviders.size() == 0) {
            if (mCompressListener != null) {
                mCompressListener.onError(new NullPointerException("image file cannot be null"));
            }
        }

        Iterator<InputStreamProvider> iterator = mStreamProviders.iterator();

        while (iterator.hasNext()) {
            final InputStreamProvider path = iterator.next();

            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START));

                        File result = compress(context, path);

                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, result));
                    } catch (IOException e) {
                        e.printStackTrace();
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, e));
                    }
                }
            });

            iterator.remove();
        }
    }

    private File compress(Context context, InputStreamProvider path) throws IOException {
        File result;

        File outFile = getImageCacheFile(context, Checker.SINGLE.extSuffix(path));

        result = Checker.SINGLE.needCompress(mLeastCompressSize, path.getPath()) ?
                new Engine(path, outFile, mFocusAlpha).compress() : new File(path.getPath());

        return result;
    }

    private File getImageCacheFile(Context context, String suffix) {
        File targetDirFile;
        if (TextUtils.isEmpty(mTargetDir)) {
            targetDirFile = getImageCacheDir(context);
        } else {
            targetDirFile = new File(mTargetDir);
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
            }
        }

        String fileName;
        if (TextUtils.isEmpty(mTargetName)) {
            fileName = System.currentTimeMillis() + (int) (Math.random() * 1000)
                    + (TextUtils.isEmpty(suffix) ? ".jpg" : suffix);
        } else {
            fileName = mTargetName + (TextUtils.isEmpty(suffix) ? ".jpg" : suffix);
        }

        return new File(targetDirFile, fileName);
    }

    private File getImageCacheDir(Context context) {
        return getImageCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    private File getImageCacheDir(Context context, String defaultDiskCacheDir) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, defaultDiskCacheDir);
            if (!result.mkdirs() && (!result.exists() || !result.isDirectory())) {
                // File wasn't able to create a directory, or the result exists but not a directory
                return null;
            }
            return result;
        }
        if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "default disk cache dir is null");
        }
        return null;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mCompressListener == null) {
            return false;
        }

        switch (msg.what) {
            case MSG_COMPRESS_START:
                mCompressListener.onStart();
                break;
            case MSG_COMPRESS_SUCCESS:
                mCompressListener.onSuccess((File) msg.obj);
                break;
            case MSG_COMPRESS_ERROR:
                mCompressListener.onError((Throwable) msg.obj);
                break;
            default:
                break;
        }
        return false;
    }

    public static class Builder {
        private Context context;
        private List<InputStreamProvider> mStreamProviders;
        private OnCompressListener mCompressListener;
        private String mTargetDir;
        private String mTargetName;
        private boolean mFocusAlpha;
        private int mLeastCompressSize = 100;

        public Builder(Context context) {
            this.context = context;
            this.mStreamProviders = new ArrayList<>();
        }

        private Luban build() {
            return new Luban(this);
        }

        public Builder load(final File file) {
            mStreamProviders.add(new InputStreamProvider() {
                @Override
                public InputStream open() throws IOException {
                    return new FileInputStream(file);
                }

                @Override
                public String getPath() {
                    return file.getAbsolutePath();
                }
            });
            return this;
        }

        public Builder load(final String string) {
            mStreamProviders.add(new InputStreamProvider() {
                @Override
                public InputStream open() throws IOException {
                    return new FileInputStream(string);
                }

                @Override
                public String getPath() {
                    return string;
                }
            });
            return this;
        }

        public Builder load(final Uri uri) {
            mStreamProviders.add(new InputStreamProvider() {
                @Override
                public InputStream open() throws IOException {
                    return context.getContentResolver().openInputStream(uri);
                }

                @Override
                public String getPath() {
                    return uri.getPath();
                }
            });
            return this;
        }

        public <T> Builder load(List<T> list) {
            for (T src : list) {
                if (src instanceof String) {
                    load((String) src);
                } else if (src instanceof File) {
                    load((File) src);
                } else if (src instanceof Uri) {
                    load((Uri) src);
                } else {
                    throw new IllegalArgumentException("Incoming data type exception, it must be String, File, Uri or Bitmap");
                }
            }
            return this;
        }

        public Builder setCompressListener(OnCompressListener listener) {
            this.mCompressListener = listener;
            return this;
        }

        public Builder setTargetDir(String targetDir) {
            this.mTargetDir = targetDir;
            return this;
        }

        public Builder setTargetName(String targetName) {
            this.mTargetName = targetName;
            return this;
        }

        /**
         * do not compress when the origin image file size less than one value
         *
         * @param size the value of file size, unit KB, default 100K
         */
        public Builder ignoreBy(int size) {
            this.mLeastCompressSize = size;
            return this;
        }

        /**
         * Do I need to keep the image's alpha channel
         *
         * @param focusAlpha <p> true - to keep alpha channel, the compress speed will be slow. </p>
         *                   <p> false - don't keep alpha channel, it might have a black background.</p>
         */
        public Builder setFocusAlpha(boolean focusAlpha) {
            this.mFocusAlpha = focusAlpha;
            return this;
        }

        public void launch() {
            build().launch(context);
        }
    }
}
