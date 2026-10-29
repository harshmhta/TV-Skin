package dev.cleanframe.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteKeysTest {
    @Test public void protectedRemoteButtonsCannotBeAssigned(){
        for(int code:new int[]{0,3,4,19,20,21,22,23,24,25,26,66,84,85,86,87,88,89,90,91,111,164,170+8,219,223,224,231})assertFalse("code="+code,RemoteKeys.canBind(code));
    }
    @Test public void standardAppAndVendorKeysCanBeAssigned(){
        for(int code:new int[]{289,290,291,292,293,294,1001,1002})assertTrue(RemoteKeys.canBind(code));
    }
}
