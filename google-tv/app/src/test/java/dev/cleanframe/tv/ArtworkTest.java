package dev.cleanframe.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkTest {
    @Test public void requestsFullResolutionFromServicesThatExposeImageSizing(){
        assertEquals("https://prod-ripcut-delivery.disney-plus.net/v1/variant/x?width=3840&height=2160",Artwork.best("https://prod-ripcut-delivery.disney-plus.net/v1/variant/x?width=544&height=306"));
        assertEquals("https://x.bamgrid.com/art?width=3840&height=2160",Artwork.best("https://x.bamgrid.com/art?width=544&height=306"));
        assertEquals("https://x.mzstatic.com/image/thumb/a/3840x2160.jpg",Artwork.best("https://x.mzstatic.com/image/thumb/a/1920x1080.jpg"));
        assertEquals("https://images-amazon.com/images/a._UR3840,2160_.jpg",Artwork.best("https://images-amazon.com/images/a._UR1920,1080_.jpg"));
        assertEquals("https://hulu.com/user/v3/artwork/a?size=3840x2160",Artwork.best("https://hulu.com/user/v3/artwork/a?size=378x210"));
    }
    @Test public void youtubeUsesItsLargestPublishedThumbnailAndOriginalUrlsRemainFallbacks(){
        assertEquals("https://i.ytimg.com/vi/video/maxresdefault.jpg",Artwork.best("https://i.ytimg.com/vi/video/hqdefault.jpg"));
        assertEquals("content://dev.astra.tv.previews/art/id",Artwork.best("content://dev.astra.tv.previews/art/id"));
    }
    @Test public void stremioUsesTheSelectedTitlesLandscapeRatherThanItsPortraitPoster(){
        assertEquals("https://images.metahub.space/background/large/tt0242519/img",Artwork.best("com.stremio.one","stremio:///detail/movie/tt0242519","https://image.tmdb.org/t/p/w600_and_h900_bestv2/poster.jpg"));
        assertEquals("https://images.metahub.space/background/large/tt0242519/img",Artwork.best("com.stremio.one","","https://images.metahub.space/poster/medium/tt0242519/img"));
        assertEquals("https://example.com/channel.jpg",Artwork.best("com.stremio.one","stremio:///channel/live","https://example.com/channel.jpg"));
        assertEquals("https://example.com/poster.jpg",Artwork.best("other.app","intent:tt0242519","https://example.com/poster.jpg"));
    }
}
