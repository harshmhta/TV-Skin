package dev.cleanframe.tv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

final class AppCatalog {
    static String id(String pkg) {
        return switch (pkg) {
            case "com.google.android.youtube.tv", "com.google.android.youtube.tvunplugged" -> pkg.endsWith("tvunplugged") ? pkg : "youtube";
            case "com.netflix.ninja", "com.netflix.mediaclient" -> "netflix";
            case "com.amazon.amazonvideo.livingroom" -> "prime";
            case "com.disney.disneyplus" -> "disney";
            case "com.viki.android" -> "viki";
            case "com.wbd.stream", "com.hbo.hbonow" -> "hbo";
            case "com.tubitv" -> "tubi";
            case "com.apple.atve.androidtv.appletv" -> "appletv";
            case "dev.astra.tv" -> "astra";
            default -> pkg;
        };
    }

    static List<ResolveInfo> list(Context context) {
        PackageManager pm=context.getPackageManager();
        Set<String> homes=new HashSet<>();
        homes.add(context.getPackageName());
        for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0)) {
            if(info.priority>=0)homes.add(info.activityInfo.packageName);
        }
        Map<String, ResolveInfo> apps=new LinkedHashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)) {
                String pkg=info.activityInfo.packageName;
                if(homes.contains(pkg))continue;
                if(pkg.startsWith("com.google.android.tungsten."))continue;
                apps.putIfAbsent(pkg,info);
            }
        }
        List<ResolveInfo> result=new ArrayList<>(apps.values());
        result.sort(Comparator.comparing(r->r.loadLabel(pm).toString(),String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    static JSONArray json(Context context,List<ResolveInfo> apps) throws org.json.JSONException {
        JSONArray result=new JSONArray();
        for(ResolveInfo info:apps) {
            String pkg=info.activityInfo.packageName;
            result.put(new JSONObject().put("id",id(pkg)).put("pkg",pkg)
                .put("name",info.loadLabel(context.getPackageManager()).toString())
                .put("icon","https://appassets.androidplatform.net/icon/"+pkg));
        }
        return result;
    }
}
