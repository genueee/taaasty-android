package ru.taaasty.utils;

import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * ViewTreeObserver.OnPreDrawListener, выполняющийся 1 раз, когда размеры известны. С учетом особенностей ViewTreeObserver
 * Если выполнение задания уже не требуется, нужно вызвать {@link #cancelAndRemoveListener()}.
 *
 */
public class SafeOnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

    private final View mRoot;

    private RunOnLaidOut mRunnable;

    private boolean mCancelled;

    private boolean mCompleted;

    public interface RunOnLaidOut {
        /**
         *
         * @param root переданный root
         * @return Return true to proceed with the current drawing pass, or false to cancel.
         */
        boolean run(View root);
    }

    /**
     * Выполняет задание либо сразу, либо после того, как размеры view будут известны
     * @param root view, за чьими размерами следить
     * @param runnable задание
     * @return созданный SafeOnPreDrawListener. В случае, если размеры известны, то null
     */
    @Nullable
    public static SafeOnPreDrawListener runWhenLaidOut(View root, RunOnLaidOut runnable) {
        if (ViewCompat.isLaidOut(root)) {
            runnable.run(root);
            return null;
        } else {
            SafeOnPreDrawListener listener = new SafeOnPreDrawListener(root, runnable);
            root.getViewTreeObserver().addOnPreDrawListener(listener);
            return listener;
        }
    }

    public SafeOnPreDrawListener(View root, RunOnLaidOut runnable) {
        this.mRoot = root;
        this.mRunnable = runnable;
    }

    @Override
    public boolean onPreDraw() {
        if (mCompleted || mCancelled) {
            // Задание отменено, но вызвался onPreDraw().
            // После завервешения задания onPreDraw выполнятся обычно не должен, но на всякий случай, тоже добавлен
            tryRemoveObserver();
            return true;
        }

        if (ViewCompat.isLaidOut(mRoot)) {
            mCompleted = true;
            tryRemoveObserver();
            if (mRunnable == null) {
                return true;
            } else {
                boolean process =  mRunnable.run(mRoot);
                mRunnable = null;
                return process;
            }
        }

        // Размеры для нашего root неизвестны. Бывает, если root находится сейчас отсоединен от окна
        // (например, временно в RecyclerView, либо он там в состояннии recycled)

        return true;
    }

    public boolean isCompleted() {
        return mCompleted;
    }

    public boolean isCancelled() {
        return mCancelled;
    }

    public void cancelAndRemoveListener() {
        if (!mCompleted && !mCancelled) {
            mCancelled = true;
            mRunnable = null;
            tryRemoveObserver();
        }
    }

    private void tryRemoveObserver() {
        // Если root сейчас отсоединен (detached), до этого был когд-то подсоединен, то
        // слушатель будет в списке основного ViewTreeObserver, а
        // getViewObserver() вернет пустышку и мы не удалимся из нужного ViewTreeObserver.
        //
        this.mRoot.getViewTreeObserver().removeOnPreDrawListener(this);
    }
}
