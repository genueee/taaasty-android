package ru.taaasty.utils;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import ru.taaasty.BuildConfig;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class ImageUtilsTest {

    @Test
    public void testIsLightColor() throws Exception {
        Assert.assertTrue(ImageUtils.isLightColor("#ffffff"));
        Assert.assertTrue(ImageUtils.isLightColor("#fff"));
        Assert.assertTrue(!ImageUtils.isLightColor("#000000"));
        Assert.assertTrue(!ImageUtils.isLightColor("#000000"));
        Assert.assertTrue(ImageUtils.isLightColor("#c6c9cc"));
        Assert.assertTrue(!ImageUtils.isLightColor("#6c7a89"));
        Assert.assertTrue(!ImageUtils.isLightColor("#6c7a89"));
        Assert.assertTrue(!ImageUtils.isLightColor("#38434e"));
        Assert.assertTrue(!ImageUtils.isLightColor("#38434e"));
        Assert.assertTrue(!ImageUtils.isLightColor("#e74c3c"));
        Assert.assertTrue(!ImageUtils.isLightColor("blabla"));
        Assert.assertTrue(!ImageUtils.isLightColor(null));
    }
}