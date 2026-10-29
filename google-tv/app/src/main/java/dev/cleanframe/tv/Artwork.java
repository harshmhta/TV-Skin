package dev.cleanframe.tv;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asks each streaming service's image server for its largest rendition of the art an app published to the
 * TV home screen. Apps publish small sizes for the old Android TV rows (Disney+ 544x306, Hulu 378x210); the
 * same servers render 3840x2160 on request. The original stays the fallback.
 */
final class Artwork {

    private static final Pattern METAHUB = Pattern.compile("images\\.metahub\\.space/\\w+/\\w+/(tt\\d+)/img");
    private static final Pattern IMDB_ID = Pattern.compile("tt\\d{5,10}");

    private Artwork() {
    }

    static String best(String pkg,String intent,String url) {
        if("com.stremio.one".equals(pkg)) {
            Matcher id=IMDB_ID.matcher(intent==null?"":intent);
            if(id.find())return "https://images.metahub.space/background/large/"+id.group()+"/img";
            Matcher art=METAHUB.matcher(url==null?"":url);
            if(art.find())return "https://images.metahub.space/background/large/"+art.group(1)+"/img";
        }
        return best(url);
    }

    static String best(String url) {
        if (url == null || url.isEmpty()) return url;
        if (url.contains("mzstatic.com/image/thumb/")) {                    // Apple TV: .../1920x1080.jpg
            return url.replaceAll("/\\d+x\\d+(\\w*)\\.(jpg|jpeg|png|webp)", "/3840x2160$1.$2");
        }
        if (url.contains(".bamgrid.com/") || url.contains(".disney-plus.net/")) {                                  // Disney+: ...&height=306&width=544
            return url.replaceAll("width=\\d+", "width=3840").replaceAll("height=\\d+", "height=2160")
                    .replace("format=webp", "format=jpeg");
        }
        if (url.contains("hulu.com/user/v3/artwork/")) {                     // Hulu: ...&size=378x210
            if (url.contains("size=")) return url.replaceAll("size=\\d+x\\d+", "size=3840x2160");
            return url + (url.contains("?") ? "&" : "?") + "size=3840x2160";
        }
        if (url.contains("hotstarext.com/image/upload/")) {                   // JioHotstar: f_auto,w_1920
            return url.replaceAll("w_\\d+", "w_3840");
        }
        if (url.matches("https?://[^/]*(images-amazon|media-amazon)\\.com/images/.*")) {   // Prime Video: ._UR1920,1080_.jpg
            return url.replaceAll("\\._[^/]*_\\.(jpg|jpeg|png)$", "._UR3840,2160_.$1");
        }
        if (url.contains("ytimg.com/vi/")) {                                  // YouTube: 4:3 sddefault -> 16:9 maxres
            return url.replaceAll("/(?:sd|hq|mq)?default\\.jpg", "/maxresdefault.jpg");
        }
        if (url.contains("images.metahub.space/")) {                          // Stremio
            return url.replace("/small/", "/large/").replace("/medium/", "/large/");
        }
        return url;
    }

    /** Stremio art carries the IMDb id, and metahub has that title's logo. */
    static String logo(String url) {
        if (url == null) return null;
        Matcher m = METAHUB.matcher(url);
        return m.find() ? "https://images.metahub.space/logo/medium/" + m.group(1) + "/img" : null;
    }
}
