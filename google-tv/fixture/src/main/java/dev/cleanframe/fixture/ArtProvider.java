package dev.cleanframe.fixture;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

public final class ArtProvider extends ContentProvider {
    @Override public boolean onCreate(){
        try(FileOutputStream out=new FileOutputStream(new File(getContext().getFilesDir(),"preview.png"))){Bitmap b=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.rgb(32,75,95));Paint p=new Paint();p.setColor(Color.WHITE);p.setTextSize(48);c.drawText("App-published preview fixture",80,360,p);b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();return true;}catch(Exception e){return false;}
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {if(!"r".equals(mode)||!"/preview".equals(uri.getPath()))throw new FileNotFoundException();return ParcelFileDescriptor.open(new File(getContext().getFilesDir(),"preview.png"),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public String getType(Uri uri){return "image/png";}
    @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String o){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
