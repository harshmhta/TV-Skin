package dev.cleanframe.tv;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public final class RemoteButtonService extends AccessibilityService {
    @Override protected boolean onKeyEvent(KeyEvent event){return RemoteKeys.handle(this,event);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
}
