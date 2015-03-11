package ru.taaasty.utils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static ru.taaasty.utils.UiUtils.getLastUrl;
import static ru.taaasty.utils.UiUtils.trimLastUrl;

public class UiUtilsTest {

    @Test
    public void testGetLastUrl() throws Exception {
        assertNull(getLastUrl(null));
        assertEquals("http://localhost", getLastUrl("http://localhost"));
        assertEquals("http://localhost", getLastUrl(" http://localhost "));
        assertEquals("http://localhost", getLastUrl("блабла http://localhost"));
        assertEquals("http://localhost", getLastUrl("http://localhost блабла"));
        assertEquals("http://localhost", getLastUrl("блабла http://ya.ru блабла http://localhost блабла"));
        assertEquals("http://localhost", getLastUrl("блабла http://ya.ru блабла http://localhost"));
        assertEquals("http://localhost", getLastUrl("http://ya.ru блабла http://localhost"));
        assertEquals("http://youtubecreator.blogspot.ru/2015/03/a-new-way-to-see-and-share-your-world.html",
                getLastUrl("http://youtubecreator.blogspot.ru/2015/03/a-new-way-to-see-and-share-your-world.html"));
    }

    @Test
    public void testTrimLastUrl() throws Exception {
        assertEquals("", trimLastUrl("http://localhost").toString());
        assertEquals("  ", trimLastUrl(" http://localhost ").toString());
        assertEquals("блабла ", trimLastUrl("блабла http://localhost").toString());
        assertEquals(" блабла", trimLastUrl("http://localhost блабла").toString());
        assertEquals("блабла http://ya.ru блабла  блабла", trimLastUrl("блабла http://ya.ru блабла http://localhost блабла").toString());
        assertEquals("блабла http://ya.ru блабла ", trimLastUrl("блабла http://ya.ru блабла http://localhost").toString());
        assertEquals("http://ya.ru блабла ", trimLastUrl("http://ya.ru блабла http://localhost").toString());
        assertEquals("",
                trimLastUrl("http://youtubecreator.blogspot.ru/2015/03/a-new-way-to-see-and-share-your-world.html").toString());
    }
}