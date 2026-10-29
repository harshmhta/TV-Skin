package dev.cleanframe.tv;

final class HisenseKeys {
    // From this TV's Smartsense GlobalKeyReceiver and window-policy key map.
    static String app(int code){return switch(code){
        case 190,293->"com.google.android.youtube.tv";
        case 295->"com.amazon.amazonvideo.livingroom";
        case 363,607->"com.disney.disneyplus";
        case 172->"com.vt.tv.tvcenter";
        case 288->"@accounts";
        case 82,83,176->"@settings";
        case 1593->"";
        default->null;
    };}
}
