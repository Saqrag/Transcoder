package net.ypresto.androidtranscoder.strategy;

/**
 * Contains presets and utilities for defining a {@link DefaultVideoStrategy}.
 */
public class DefaultVideoStrategies {

    private DefaultVideoStrategies() {}

    /**
     * A {@link DefaultVideoStrategy} that uses 720x1280.
     * This preset is ensured to work on any Android &gt;=4.3 devices by Android CTS,
     * assuming that the codec is available.
     */
    public static DefaultVideoStrategy for720x1280() {
        return DefaultVideoStrategy.builder(720, 1280)
                .bitRate(2L * 1000 * 1000)
                .frameRate(30)
                .iFrameInterval(3F)
                .build();
    }

    /**
     * A {@link DefaultVideoStrategy} that uses 360x480 (3:4),
     * ensured to work for 3:4 videos as explained by
     * https://developer.android.com/guide/topics/media/media-formats
     */
    public static DefaultVideoStrategy for360x480() {
        return DefaultVideoStrategy.builder(360, 480)
                .bitRate(500L * 1000)
                .frameRate(30)
                .iFrameInterval(3F)
                .build();
    }
}