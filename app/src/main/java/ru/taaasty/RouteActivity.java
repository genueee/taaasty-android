package ru.taaasty;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.ShowPostActivity;

public class RouteActivity extends Activity {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "RouteActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (DBG) Log.v(TAG, "data: " + data);

        List<String> segments = data.getPathSegments();
        String slug = segments.get(0).substring(1);

        if (segments.size() == 1) {
            TlogActivity.startTlogActivity(this, slug, null);
            finish();
            return;
        }

        String seg2 = segments.get(1);
        Matcher postIdMather = Pattern.compile("^(\\d+)(?:-.+)?").matcher(seg2);
        if (postIdMather.matches()) {
            long postId = Long.parseLong(postIdMather.group(1));
            new ShowPostActivity.Builder(this)
                    .setEntryId(postId)
                    .setShowFullPost(true)
                    .startActivity();
            finish();
            return;
        }

        // TODO открытие комментария и ещё всякой хрени

        // Не подерживаем. Открываем в браузере
        Intent showIntent = new Intent(Intent.ACTION_VIEW, data);
        PackageManager manager = getPackageManager();
        List<ResolveInfo> list = manager. queryIntentActivities(showIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info: list) {
            if (!BuildConfig.APPLICATION_ID.equals(info.activityInfo.packageName)) {
                if (DBG) Log.v(TAG, "package name: " + info.activityInfo.packageName + " my name: " + BuildConfig.APPLICATION_ID);
                showIntent.setClassName(info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name);
                startActivity(showIntent);
                break;
            }
        }

        finish();
    }
}
