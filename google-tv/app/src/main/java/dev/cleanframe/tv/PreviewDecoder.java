package dev.cleanframe.tv;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Keeps the same UHD decoder and display surface between still images. */
final class PreviewDecoder {
    private record Pending(BooleanSupplier valid,Runnable ready,AtomicBoolean notified) {}
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<Long,Pending> frames=new ConcurrentHashMap<>();
    private MediaCodec codec;
    private long pts;
    private volatile boolean closed;

    void show(File file,Surface surface,BooleanSupplier valid,Runnable ready,Runnable failed) {
        if(closed)return;
        worker.execute(()->{
            if(!valid.getAsBoolean())return;
            MediaExtractor extractor=new MediaExtractor();
            try {
                extractor.setDataSource(file.getAbsolutePath());extractor.selectTrack(0);
                if(codec==null) {
                    MediaFormat format=extractor.getTrackFormat(0);
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,8*1024*1024);
                    codec=MediaCodec.createDecoderByType("video/avc");
                    codec.configure(format,surface,null,0);
                    codec.setOnFrameRenderedListener((c,time,nanos)->{
                        Pending pending=frames.remove(time);
                        if(pending!=null&&pending.valid().getAsBoolean()&&pending.notified().compareAndSet(false,true)) {
                            frames.values().removeIf(p->p==pending);
                            Log.i("CleanFrame4K","Presented still frame");
                            pending.ready().run();
                        }
                    },main);
                    codec.start();
                    Log.i("CleanFrame4K","Started persistent decoder="+codec.getName());
                }
                Pending pending=new Pending(valid,ready,new AtomicBoolean());
                MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
                int queued=0,rendered=0;
                Set<Long> requested=new HashSet<>();
                long deadline=System.nanoTime()+2_000_000_000L;
                while(valid.getAsBoolean()&&System.nanoTime()<deadline) {
                    if(queued<2) {
                        int index=codec.dequeueInputBuffer(10_000);
                        if(index>=0) {
                            ByteBuffer input=codec.getInputBuffer(index);input.clear();
                            int size=extractor.readSampleData(input,0);
                            if(size<0)throw new IllegalStateException("Missing still frame");
                            long timestamp=pts+=33_333L;frames.put(timestamp,pending);requested.add(timestamp);
                            codec.queueInputBuffer(index,0,size,timestamp,extractor.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_SYNC);
                            extractor.advance();queued++;
                        }
                    }
                    int index=codec.dequeueOutputBuffer(info,10_000);
                    if(index>=0) {codec.releaseOutputBuffer(index,true);if(requested.remove(info.presentationTimeUs))rendered++;}
                    else if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat output=codec.getOutputFormat();
                        int width=output.containsKey("crop-right")?output.getInteger("crop-right")-output.getInteger("crop-left")+1:output.getInteger(MediaFormat.KEY_WIDTH);
                        int height=output.containsKey("crop-bottom")?output.getInteger("crop-bottom")-output.getInteger("crop-top")+1:output.getInteger(MediaFormat.KEY_HEIGHT);
                        Log.i("CleanFrame4K","Decoder output="+width+"x"+height);
                    }
                    if(queued==2&&rendered>=2)break;
                }
                if(valid.getAsBoolean()&&rendered==0)throw new IllegalStateException("No decoded frame");
            } catch(Exception e) {
                Log.w("CleanFrame4K","Still decoder failed: "+e.getClass().getSimpleName());
                releaseCodec();
                main.post(()->{if(valid.getAsBoolean())failed.run();});
            } finally {extractor.release();}
        });
    }
    private void releaseCodec() {
        if(codec!=null){try{codec.release();}catch(RuntimeException ignored){}codec=null;}
        frames.clear();
    }
    void clear(){if(!closed)worker.execute(this::releaseCodec);}
    void release(){closed=true;worker.execute(this::releaseCodec);worker.shutdown();}
}
