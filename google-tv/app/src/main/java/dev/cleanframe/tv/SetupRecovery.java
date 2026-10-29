package dev.cleanframe.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

/** Restores Android's required setup through the app after a data reset. */
final class SetupRecovery {
    static final String TV_LISTINGS="android.permission.READ_TV_LISTINGS";
    private static final int PREVIEW_REQUEST=42;
    private final Activity activity;
    private final Runnable changed;
    private boolean active,ready,requesting,showing;

    SetupRecovery(Activity activity,Runnable changed){this.activity=activity;this.changed=changed;}
    boolean hasPreviewAccess(){return activity.checkSelfPermission(TV_LISTINGS)==PackageManager.PERMISSION_GRANTED;}
    void pageReady(){ready=true;check();}
    void resume(){active=true;check();}
    void pause(){active=false;}

    private boolean remoteEnabled(){
        String services=Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(services==null)return false;
        ComponentName own=new ComponentName(activity,RemoteButtonService.class);
        for(String service:services.split(":"))if(own.equals(ComponentName.unflattenFromString(service)))return true;
        return false;
    }
    private void check(){
        if(!active||!ready||requesting||showing||activity.isFinishing())return;
        if(!hasPreviewAccess()&&!activity.getPreferences(0).getBoolean("setup.previewAsked",false)){requestPreviewAccess();return;}
        if(!remoteEnabled()&&!activity.getPreferences(0).getBoolean("setup.remoteAsked",false)){
            activity.getPreferences(0).edit().putBoolean("setup.remoteAsked",true).apply();showing=true;
            new AlertDialog.Builder(activity)
                .setTitle("Enable remote app buttons")
                .setMessage("Turn on CleanFrame app buttons in Accessibility to use the remote's app shortcuts while watching other apps.")
                .setPositiveButton("Open Accessibility",(dialog,which)->openRemoteSettings())
                .setNegativeButton("Later",null)
                .setOnDismissListener(dialog->showing=false).show();
        }
    }
    void requestPreviewAccess(){
        if(hasPreviewAccess()){changed.run();return;}
        if(requesting)return;
        if(activity.getPreferences(0).getBoolean("setup.previewAsked",false)&&!activity.shouldShowRequestPermissionRationale(TV_LISTINGS)){
            activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));return;
        }
        activity.getPreferences(0).edit().putBoolean("setup.previewAsked",true).apply();requesting=true;
        activity.requestPermissions(new String[]{TV_LISTINGS},PREVIEW_REQUEST);
    }
    void permissionResult(int code){if(code==PREVIEW_REQUEST){requesting=false;changed.run();activity.getWindow().getDecorView().post(this::check);}}
    private void openRemoteSettings(){
        activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
