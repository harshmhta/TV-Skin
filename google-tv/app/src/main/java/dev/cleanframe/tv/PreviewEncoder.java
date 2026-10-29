package dev.cleanframe.tv;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.io.File;
import java.nio.ByteBuffer;

final class PreviewEncoder {
    static {System.loadLibrary("preview_encoder");}
    private static native byte[][] encode(Bitmap bitmap);

    static void write(Bitmap bitmap,File output)throws Exception {
        byte[][] chunks=encode(bitmap);
        if(chunks==null||chunks.length!=4)throw new IllegalStateException("UHD image encoding failed");
        for(byte[] chunk:chunks)if(chunk==null)throw new IllegalStateException("Missing AVC frame data");
        MediaMuxer muxer=new MediaMuxer(output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        try {
            MediaFormat format=MediaFormat.createVideoFormat("video/avc",3840,2160);
            format.setInteger(MediaFormat.KEY_FRAME_RATE,1);
            format.setByteBuffer("csd-0",ByteBuffer.wrap(chunks[0]));format.setByteBuffer("csd-1",ByteBuffer.wrap(chunks[1]));
            int track=muxer.addTrack(format);muxer.start();
            for(int i=2;i<4;i++){byte[] sample=AvcSamples.forMuxer(chunks[i]);MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();info.set(0,sample.length,(i-2)*1000000L,i==2?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);muxer.writeSampleData(track,ByteBuffer.wrap(sample),info);}
            muxer.stop();
        }finally{muxer.release();}
    }
}
