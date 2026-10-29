package dev.cleanframe.tv;
import org.junit.Test;
import static org.junit.Assert.*;

public class HisenseKeysTest {
    @Test public void routesFromThisFirmwareOpenInstalledAppPackages(){
        assertEquals("com.google.android.youtube.tv",HisenseKeys.app(293));
        assertEquals("com.google.android.youtube.tv",HisenseKeys.app(190));
        assertEquals("com.amazon.amazonvideo.livingroom",HisenseKeys.app(295));
        assertEquals("com.disney.disneyplus",HisenseKeys.app(607));
        assertEquals("com.disney.disneyplus",HisenseKeys.app(363));
        assertEquals("com.vt.tv.tvcenter",HisenseKeys.app(172));
        assertEquals("@accounts",HisenseKeys.app(288));
        assertEquals("@settings",HisenseKeys.app(83));
        assertEquals("@settings",HisenseKeys.app(176));
    }
    @Test public void netflixAndSystemButtonsRetainTheirManufacturerHandlers(){
        for(int code:new int[]{3,4,19,20,21,22,23,24,25,26,178,191,219,231})assertNull(HisenseKeys.app(code));
    }
    @Test public void kidButtonDoesNothingAsRequested(){assertEquals("",HisenseKeys.app(1593));}
}
