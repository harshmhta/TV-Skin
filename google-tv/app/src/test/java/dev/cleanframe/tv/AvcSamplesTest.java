package dev.cleanframe.tv;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class AvcSamplesTest {
    @Test public void preservesEveryPayloadByteBetweenMixedLengthSliceDelimiters() {
        byte[] encoded={0,0,0,1,0x65,11,22,33,0,0,1,0x65,44,55,66};
        byte[] expected={0,0,0,1,0x65,11,22,33,0,0,0,1,0x65,44,55,66};
        assertArrayEquals(expected,AvcSamples.forMuxer(encoded));
    }
    @Test public void normalizesTheFirstDelimiterAndLeavesEscapedPayloadBytesUntouched() {
        byte[] encoded={0,0,1,0x65,0,0,3,1,99};
        byte[] expected={0,0,0,1,0x65,0,0,3,1,99};
        assertArrayEquals(expected,AvcSamples.forMuxer(encoded));
    }
    @Test public void keepsSamplesWithFourByteDelimitersUnchanged() {
        byte[] encoded={0,0,0,1,0x65,11,0,0,0,1,0x65,22};
        assertArrayEquals(encoded,AvcSamples.forMuxer(encoded));
    }
}
