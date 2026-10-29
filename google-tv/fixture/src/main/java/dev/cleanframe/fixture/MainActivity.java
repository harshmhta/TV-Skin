package dev.cleanframe.fixture;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(getIntent().getBooleanExtra("opened",false)){
            Log.i("CleanFrameFixture","Preview deep link opened");
            TextView text=new TextView(this);text.setText("Preview deep link opened");setContentView(text);return;
        }
        try {
            Uri channels=Uri.parse("content://android.media.tv/channel");
            getContentResolver().delete(channels,null,null);
            getContentResolver().delete(Uri.parse("content://android.media.tv/watch_next_program"),null,null);
            ContentValues channel=new ContentValues();channel.put("type","TYPE_PREVIEW");channel.put("display_name","App-published fixture");channel.put("input_id","dev.cleanframe.fixture");channel.put("searchable",1);
            Uri created=getContentResolver().insert(channels,channel);
            ContentValues program=new ContentValues();program.put("channel_id",Long.parseLong(created.getLastPathSegment()));program.put("type",0);program.put("title","Real TvProvider preview");program.put("poster_art_uri","content://dev.cleanframe.fixture.art/preview");program.put("searchable",1);
            String link=new Intent(this,MainActivity.class).setAction(Intent.ACTION_VIEW).putExtra("opened",true).toUri(Intent.URI_INTENT_SCHEME);
            program.put("intent_uri",link);
            getContentResolver().insert(Uri.parse("content://android.media.tv/preview_program"),program);
            program.remove("channel_id");program.put("title","Real Watch Next preview");program.put("watch_next_type",0);program.put("last_engagement_time_utc_millis",System.currentTimeMillis());
            getContentResolver().insert(Uri.parse("content://android.media.tv/watch_next_program"),program);
            Log.i("CleanFrameFixture","Published preview and Watch Next");
        }catch(Exception e){Log.e("CleanFrameFixture","Publish failed",e);}
        finish();
    }
}
