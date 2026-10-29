package dev.cleanframe.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.window.OnBackInvokedDispatcher;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.FrameLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String ORIGIN="https://appassets.androidplatform.net";
    private WebView web;
    private BackdropView backdrop;
    private SetupRecovery setup;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<String,String> packages=new ConcurrentHashMap<>();
    private volatile PreviewRepository repository=new PreviewRepository();
    private volatile boolean loaded;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setup=new SetupRecovery(this,this::refreshCatalog);
        getWindow().setFormat(android.graphics.PixelFormat.OPAQUE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        web=new WebView(this);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0);
        web.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setTextZoom(100);
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onConsoleMessage(ConsoleMessage m){Log.d("CleanFrameWeb",m.message()+" @"+m.lineNumber());return true;}
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){return resource(request.getUrl());}
            @Override public void onPageFinished(WebView v,String url){loaded=true;js("androidResume()");setup.pageReady();}
        });
        web.addJavascriptInterface(new Bridge(),"Android");
        FrameLayout root=new FrameLayout(this);
        backdrop=new BackdropView(this,(key,visible)->js("nativeBackdropReady("+JSONObject.quote(key)+","+visible+")"));
        root.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        web.loadUrl(ORIGIN+"/index.html");
        web.requestFocus();
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->js("key('back')"));
    }

    private WebResourceResponse resource(Uri uri) {
        if(!ORIGIN.equals(uri.getScheme()+"://"+uri.getHost()))return null;
        try {
            String path=uri.getPath();
            if(path!=null&&path.startsWith("/icon/")) {
                String pkg=path.substring(6);
                if(!packages.containsValue(pkg))return empty();
                Drawable d=getPackageManager().getApplicationIcon(pkg);
                Bitmap b=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
                d.setBounds(0,0,128,128);d.draw(new Canvas(b));
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,bytes);b.recycle();
                return new WebResourceResponse("image/png",null,new ByteArrayInputStream(bytes.toByteArray()));
            }
            if("/art".equals(path)) {
                String source=uri.getQueryParameter("uri");
                if(source==null||!repository.artUris.contains(source)||!source.startsWith("content://"))return empty();
                return new WebResourceResponse("image/*",null,getContentResolver().openInputStream(Uri.parse(source)));
            }
            if("/index.html".equals(path))return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));
            if("/fonts/Manrope.ttf".equals(path))return new WebResourceResponse("font/ttf",null,getAssets().open("fonts/Manrope.ttf"));
            if("/backgrounds/all-apps.jpg".equals(path))return new WebResourceResponse("image/jpeg",null,getAssets().open("backgrounds/all-apps.jpg"));
        }catch(Exception e){Log.w("CleanFrame","Local resource unavailable",e);}
        return empty();
    }
    private static WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    private void js(String script){runOnUiThread(()->{if(web!=null)web.evaluateJavascript(script,null);});}

    private void refreshCatalog(){
        worker.execute(()->{
            try {
                List<ResolveInfo> apps=AppCatalog.list(this);packages.clear();
                for(ResolveInfo app:apps)packages.put(AppCatalog.id(app.activityInfo.packageName),app.activityInfo.packageName);
                PreviewRepository next=new PreviewRepository();
                JSONObject previews=next.load(this,new HashSet<>(packages.values()));repository=next;
                String saved=getPreferences(0).getString("pins",null);
                JSONObject data=new JSONObject().put("apps",AppCatalog.json(this,apps)).put("previews",previews)
                    .put("pins",saved==null?JSONObject.NULL:new JSONArray(saved)).put("previewAccess",setup.hasPreviewAccess());
                Log.i("CleanFrame","Apps="+apps.size()+" previewApps="+previews.length());
                js("setCatalog("+data+")");
            }catch(Exception e){Log.e("CleanFrame","Catalog failed",e);}
        });
    }

    private void launchPackage(String pkg){
        Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(pkg);
        if(intent!=null){try{startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;}catch(Exception e){Log.w("CleanFrame","App launch failed",e);}}
        Toast.makeText(this,"Couldn't open this app",Toast.LENGTH_SHORT).show();
    }

    private void openProgram(String key){
        PreviewRepository.Program item=repository.programs.get(key);if(item==null)return;
        try {
            if(!item.intent().isEmpty()) {
                Intent supplied=Intent.parseUri(item.intent(),Intent.URI_INTENT_SCHEME);
                // Preserve the publisher's detail/playback extras within its own package.
                if(supplied.getComponent()!=null&&!item.pkg().equals(supplied.getComponent().getPackageName()))supplied.setComponent(null);
                supplied.setSelector(null);supplied.setClipData(null);supplied.setPackage(item.pkg());
                supplied.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if(supplied.getAction()==null)supplied.setAction(Intent.ACTION_VIEW);
                startActivity(supplied);return;
            }
        }catch(Exception e){Log.i("CleanFrame","Preview deep link unavailable");}
        launchPackage(item.pkg());
    }

    private void openSetting(String name){
        String action="inputs".equals(name)?"com.android.tv.action.VIEW_INPUTS":Settings.ACTION_SETTINGS;
        try{startActivity(new Intent(action));}catch(Exception e){
            if("inputs".equals(name))Toast.makeText(this,"TV input picker unavailable",Toast.LENGTH_SHORT).show();
            else Toast.makeText(this,"TV settings unavailable",Toast.LENGTH_SHORT).show();
        }
    }

    private final class Bridge {
        @JavascriptInterface public void refresh(){runOnUiThread(()->refreshCatalog());}
        @JavascriptInterface public boolean nativeBackdrop(){return true;}
        @JavascriptInterface public void requestPreviewAccess(){runOnUiThread(setup::requestPreviewAccess);}
        @JavascriptInterface public String loadWeather(){return getPreferences(0).getString("weather","");}
        @JavascriptInterface public void saveWeather(String json){
            try{if(json.length()>2048)return;JSONObject data=new JSONObject(json);if(!Double.isFinite(data.optDouble("temperature"))||data.optLong("updatedAt")<=0)return;getPreferences(0).edit().putString("weather",data.toString()).apply();}catch(Exception ignored){}
        }
        @JavascriptInterface public void showBackdrop(String key){
            if("stock".equals(key)){backdrop.show("stock","stock","stock");return;}
            PreviewRepository.Program p=repository.programs.get(key);
            if(p!=null)backdrop.show(key,p.art(),p.fallback());
            else backdrop.show("stock","stock","stock");
        }
        @JavascriptInterface public void prefetchBackdrop(String key){
            if("stock".equals(key)){backdrop.prefetch("stock","stock","stock");return;}
            PreviewRepository.Program p=repository.programs.get(key);
            if(p!=null)backdrop.prefetch(key,p.art(),p.fallback());
        }
        @JavascriptInterface public void savePins(String json){try{JSONArray list=new JSONArray(json);JSONArray safe=new JSONArray();HashSet<String> seen=new HashSet<>();for(int i=0;i<list.length()&&safe.length()<7;i++){String id=list.getString(i);if(packages.containsKey(id)&&seen.add(id))safe.put(id);}getPreferences(0).edit().putString("pins",safe.toString()).apply();}catch(Exception e){Log.w("CleanFrame","Invalid dock order");}}
        @JavascriptInterface public void launchApp(String id){String pkg=packages.get(id);if(pkg!=null)runOnUiThread(()->launchPackage(pkg));}
        @JavascriptInterface public void openProgram(String key){runOnUiThread(()->MainActivity.this.openProgram(key));}
        @JavascriptInterface public void openSetting(String name){runOnUiThread(()->MainActivity.this.openSetting(name));}
    }

    @android.annotation.SuppressLint("GestureBackNavigation") // Android TV 8-12 remotes deliver physical Back key events.
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(RemoteKeys.handle(this,event))return true;
        String key=switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_DPAD_LEFT->"left";case KeyEvent.KEYCODE_DPAD_RIGHT->"right";
            case KeyEvent.KEYCODE_DPAD_UP->"up";case KeyEvent.KEYCODE_DPAD_DOWN->"down";
            case KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_NUMPAD_ENTER->"ok";
            case KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_ESCAPE->"back";
            default->null;
        };
        if(key!=null){js("androidKey("+JSONObject.quote(key)+","+JSONObject.quote(event.getAction()==KeyEvent.ACTION_DOWN?"down":"up")+","+event.getRepeatCount()+")");return true;}
        if(event.getKeyCode()==KeyEvent.KEYCODE_MENU||event.getKeyCode()==KeyEvent.KEYCODE_SETTINGS){if(event.getAction()==KeyEvent.ACTION_UP)openSetting("tv");return true;}
        if(isMediaKey(event.getKeyCode())){((AudioManager)getSystemService(AUDIO_SERVICE)).dispatchMediaKeyEvent(event);return true;}
        return super.dispatchKeyEvent(event);
    }
    private static boolean isMediaKey(int code){return switch(code){case KeyEvent.KEYCODE_MEDIA_PLAY,KeyEvent.KEYCODE_MEDIA_PAUSE,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_MEDIA_STOP,KeyEvent.KEYCODE_MEDIA_NEXT,KeyEvent.KEYCODE_MEDIA_PREVIOUS,KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,KeyEvent.KEYCODE_MEDIA_REWIND->true;default->false;};}
    @android.annotation.SuppressLint("GestureBackNavigation") // API 26-32 fallback; API 33+ callback is registered above.
    @Override public void onBackPressed(){js("key('back')");}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);js("androidHome()");}
    @Override protected void onResume(){super.onResume();if(backdrop!=null)backdrop.resume();if(web!=null){web.onResume();web.resumeTimers();if(loaded){js("androidResume()");refreshCatalog();}}if(setup!=null)setup.resume();}
    @Override protected void onPause(){if(setup!=null)setup.pause();js("androidPause()");if(backdrop!=null)backdrop.pause();if(web!=null){web.onPause();web.pauseTimers();}super.onPause();}
    @Override protected void onDestroy(){loaded=false;worker.shutdownNow();if(backdrop!=null)backdrop.release();if(web!=null){web.removeJavascriptInterface("Android");web.destroy();web=null;}super.onDestroy();}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);setup.permissionResult(code);}
}
