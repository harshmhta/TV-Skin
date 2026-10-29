package dev.cleanframe.tv;

import java.io.ByteArrayOutputStream;

final class AvcSamples {
    static byte[] forMuxer(byte[] sample) {
        // Android 12's MPEG4Writer subtracts four bytes at every NAL boundary.
        // x264 uses three bytes for later slices, which otherwise truncates a payload.
        ByteArrayOutputStream output=null;int start=0;
        for(int i=0;i<sample.length;) {
            if(i+3<sample.length&&sample[i]==0&&sample[i+1]==0&&sample[i+2]==0&&sample[i+3]==1) {
                i+=4;
            } else if(i+2<sample.length&&sample[i]==0&&sample[i+1]==0&&sample[i+2]==1) {
                if(output==null)output=new ByteArrayOutputStream(sample.length+16);
                output.write(sample,start,i-start);output.write(0);start=i;i+=3;
            } else i++;
        }
        if(output==null)return sample;
        output.write(sample,start,sample.length-start);
        return output.toByteArray();
    }
}
