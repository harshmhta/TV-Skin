package dev.cleanframe.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class BackdropView extends SurfaceView implements SurfaceHolder.Callback {
    interface Listener {void ready(String key,boolean visible);}
    private final ExecutorService loader=Executors.newSingleThreadExecutor(r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"UhdArtwork"));
    private final AtomicInteger epoch=new AtomicInteger();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final String userAgent;
    private final PreviewDecoder decoder=new PreviewDecoder();
    private File currentFile;
    private String currentKey="";
    private volatile boolean surfaceReady,active=true;
    private volatile int surfaceVersion;
    private volatile HttpURLConnection downloading;

    BackdropView(Context context,Listener listener){
        super(context);this.listener=listener;
        userAgent=android.webkit.WebSettings.getDefaultUserAgent(context);
        getHolder().setFixedSize(3840,2160);
        getHolder().addCallback(this);
        getHolder().setKeepScreenOn(false);
    }
    void show(String key,String source,String fallback){
        int token=epoch.incrementAndGet();
        HttpURLConnection old=downloading;if(old!=null)old.disconnect();
        main.post(()->{currentKey=key;currentFile=null;});
        prepare(key,source,fallback,token,true);
    }
    void prefetch(String key,String source,String fallback){prepare(key,source,fallback,epoch.get(),false);}
    private void prepare(String key,String source,String fallback,int token,boolean display){
        loader.execute(()->{
            if(token!=epoch.get()||!active)return;
            Bitmap bitmap=null,frame=null;
            File temporary=null;
            try {
                File directory=new File(getContext().getCacheDir(),"uhd-previews-v5");directory.mkdirs();
                String identity=source.startsWith("content://")?key+"\n"+source:source;
                byte[] digest=MessageDigest.getInstance("SHA-256").digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hash=new StringBuilder();for(byte value:digest)hash.append(String.format("%02x",value));
                File clip=new File(directory,hash+".mp4");
                if(!clip.exists()){
                    bitmap=load(source);
                    if(token!=epoch.get()||!active)return;
                    if(bitmap==null&&!source.equals(fallback))bitmap=load(fallback);
                    if(token!=epoch.get()||!active)return;
                    if(bitmap==null)bitmap=load("stock");
                    if(bitmap==null)throw new IllegalStateException("No image available");
                    if(token!=epoch.get()||!active)return;
                    Log.i("CleanFrame4K","Encoding source="+bitmap.getWidth()+"x"+bitmap.getHeight());
                    if(bitmap.getWidth()==3840&&bitmap.getHeight()==2160){frame=bitmap;bitmap=null;}
                    else{
                        frame=Bitmap.createBitmap(3840,2160,Bitmap.Config.ARGB_8888);
                        frame.eraseColor(0xff0b0f12);
                        Canvas canvas=new Canvas(frame);float scale=Math.max(3840f/bitmap.getWidth(),2160f/bitmap.getHeight());
                        float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
                        canvas.drawBitmap(bitmap,null,new RectF((3840-w)/2,(2160-h)/2,(3840+w)/2,(2160+h)/2),new Paint(Paint.FILTER_BITMAP_FLAG));
                        bitmap.recycle();bitmap=null;
                    }
                    temporary=new File(directory,hash+".tmp");
                    if(token!=epoch.get()||!active)return;
                    long encodeStart=android.os.SystemClock.elapsedRealtime();
                    PreviewEncoder.write(frame,temporary);
                    Log.i("CleanFrame4K","Encoded UHD in "+(android.os.SystemClock.elapsedRealtime()-encodeStart)+" ms");
                    if(!temporary.renameTo(clip))throw new IllegalStateException("Cannot save UHD preview");
                }
                clip.setLastModified(System.currentTimeMillis());
                File[] clips=directory.listFiles((dir,name)->name.endsWith(".mp4"));
                if(clips!=null&&clips.length>32){Arrays.sort(clips,Comparator.comparingLong(File::lastModified));for(int i=0;i<clips.length-32;i++)if(!clips[i].equals(clip))clips[i].delete();}
                if(display)main.post(()->{if(token==epoch.get()){currentKey=key;currentFile=clip;startClip(token);}});
            }catch(Exception|LinkageError|OutOfMemoryError e){
                Log.w("CleanFrame4K","UHD preview unavailable: "+e.getClass().getSimpleName());
                if(temporary!=null)temporary.delete();
                if(display)main.post(()->{if(token==epoch.get())listener.ready(key,false);});
            }finally{if(bitmap!=null)bitmap.recycle();if(frame!=null)frame.recycle();}
        });
    }
    private Bitmap load(String source){
        HttpURLConnection connection=null;
        try {
            InputStream input;
            if(source.equals("stock"))input=getContext().getAssets().open("backgrounds/all-apps.jpg");
            else if(source.startsWith("content://"))input=getContext().getContentResolver().openInputStream(Uri.parse(source));
            else if(source.startsWith("https://")){
                connection=(HttpURLConnection)new URL(source).openConnection();connection.setConnectTimeout(6000);connection.setReadTimeout(10000);
                downloading=connection;
                connection.setRequestProperty("User-Agent",userAgent);connection.setRequestProperty("Accept","image/jpeg,image/png,image/webp,*/*");input=connection.getInputStream();
            }else return null;
            try(InputStream in=input){BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;return BitmapFactory.decodeStream(in,null,options);}
        }catch(Exception e){return null;}finally{if(connection!=null)connection.disconnect();downloading=null;}
    }
    private void startClip(int token){
        if(!active||!surfaceReady||currentFile==null)return;
        int version=surfaceVersion;String key=currentKey;
        decoder.show(currentFile,getHolder().getSurface(),()->active&&surfaceReady&&surfaceVersion==version&&token==epoch.get(),()->listener.ready(key,true),()->listener.ready(key,false));
    }
    void pause(){active=false;epoch.incrementAndGet();decoder.clear();listener.ready(currentKey,false);}
    void resume(){active=true;}
    @Override public void surfaceCreated(SurfaceHolder holder){surfaceVersion++;surfaceReady=true;startClip(epoch.get());}
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder holder){surfaceReady=false;surfaceVersion++;decoder.clear();listener.ready(currentKey,false);}
    void release(){active=false;epoch.incrementAndGet();loader.shutdownNow();decoder.release();}
}
