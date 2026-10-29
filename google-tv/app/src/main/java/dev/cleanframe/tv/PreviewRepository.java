package dev.cleanframe.tv;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

final class PreviewRepository {
    record Program(String pkg,String intent,String art,String fallback) {}
    final Map<String,Program> programs=new LinkedHashMap<>();
    final Set<String> artUris=new HashSet<>();

    JSONObject load(Context context,Set<String> installed) throws org.json.JSONException {
        programs.clear();artUris.clear();JSONObject result=new JSONObject();
        for(String table:new String[]{"watch_next_program","preview_program"}) {
            try(Cursor c=context.getContentResolver().query(Uri.parse("content://android.media.tv/"+table),null,null,null,null)) {
                while(c!=null&&c.moveToNext()) {
                    String pkg=str(c,"package_name"),poster=str(c,"poster_art_uri"),thumb=str(c,"thumbnail_uri");
                    if(!installed.contains(pkg))continue;
                    String art=!thumb.isEmpty()?thumb:poster;
                    if(!safeImage(art))continue;
                    String id=AppCatalog.id(pkg),key=table+":"+str(c,"_id");
                    JSONArray list=result.optJSONArray(id);
                    if(list==null){list=new JSONArray();result.put(id,list);}
                    if(list.length()>=20)continue;
                    String best=Artwork.best(pkg,str(c,"intent_uri"),art);
                    programs.put(key,new Program(pkg,str(c,"intent_uri"),best,art));
                    artUris.add(art);
                    String video=str(c,"preview_video_uri");
                    list.put(new JSONObject().put("key",key).put("title",str(c,"title"))
                        .put("art",imageUrl(best)).put("fallbackArt",imageUrl(art)).put("watchNext",table.equals("watch_next_program")).put("video",video.startsWith("https://")?video:""));
                }
            }catch(SecurityException e){Log.i("CleanFrame","TV listing access not granted");}
            catch(Exception e){Log.w("CleanFrame","Cannot read "+table,e);}
        }
        return result;
    }
    static boolean safeImage(String s){return s.startsWith("https://")||s.startsWith("content://");}
    static String imageUrl(String s){return s.startsWith("content://")?"https://appassets.androidplatform.net/art?uri="+Uri.encode(s):s;}
    private static String str(Cursor c,String name){int i=c.getColumnIndex(name);return i<0||c.isNull(i)?"":c.getString(i);}
}
