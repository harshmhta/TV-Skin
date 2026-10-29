package dev.cleanframe.tv;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Build;
import android.util.Log;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;

final class RemoteKeys {
    private static String learningPackage;
    private static long learningUntil;

    static void learn(Context context,String pkg){
        learningPackage=pkg;learningUntil=SystemClock.uptimeMillis()+15000;
        Toast.makeText(context,"Press the dedicated app button within 15 seconds. Back cancels.",Toast.LENGTH_LONG).show();
    }

    static boolean canBind(int code){
        // Allow dedicated vendor/app keys. Core remote, voice, power, volume and input remain with Android.
        return code>0&&switch(code){
            case KeyEvent.KEYCODE_HOME,KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_NUMPAD_ENTER,KeyEvent.KEYCODE_POWER,KeyEvent.KEYCODE_SLEEP,KeyEvent.KEYCODE_WAKEUP,
                KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_VOLUME_DOWN,KeyEvent.KEYCODE_VOLUME_MUTE,KeyEvent.KEYCODE_MUTE,
                KeyEvent.KEYCODE_ASSIST,KeyEvent.KEYCODE_VOICE_ASSIST,KeyEvent.KEYCODE_SEARCH,KeyEvent.KEYCODE_TV_INPUT,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_MEDIA_PLAY,KeyEvent.KEYCODE_MEDIA_PAUSE,KeyEvent.KEYCODE_MEDIA_STOP,
                KeyEvent.KEYCODE_MEDIA_NEXT,KeyEvent.KEYCODE_MEDIA_PREVIOUS,KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,KeyEvent.KEYCODE_MEDIA_REWIND->false;
            default->true;
        };
    }

    static boolean handle(Context context,KeyEvent event){
        int code=event.getKeyCode();
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0&&code>KEY_NAV_LAST)Log.d("CleanFrameRemote","code="+code+" scan="+event.getScanCode());
        if(learningPackage!=null&&SystemClock.uptimeMillis()>learningUntil)learningPackage=null;
        if(learningPackage!=null&&code==KeyEvent.KEYCODE_BACK){learningPackage=null;return false;}
        if(!canBind(code))return false;
        String key="key."+code+"."+event.getScanCode();
        if(learningPackage!=null){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                context.getSharedPreferences("remote",0).edit().putString(key,learningPackage).apply();learningPackage=null;
                Toast.makeText(context,"App button saved",Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        String pkg=context.getSharedPreferences("remote",0).getString(key,null);
        if(pkg==null&&Build.MANUFACTURER.equalsIgnoreCase("Hisense"))pkg=HisenseKeys.app(code);
        if(pkg==null)return false;
        if(pkg.isEmpty())return true;
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
            if(pkg.equals("@settings")){try{context.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(RuntimeException ignored){return false;}}
            if(pkg.equals("@accounts")){try{context.startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(RuntimeException ignored){return false;}}
            Intent launch=context.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if(launch==null)launch=context.getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null)return false;
            try{context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException ignored){return false;}
        }
        return true;
    }
    private static final int KEY_NAV_LAST=26;
}
