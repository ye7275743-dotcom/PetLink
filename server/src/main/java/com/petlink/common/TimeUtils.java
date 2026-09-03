package com.petlink.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtils {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private TimeUtils() {}

    public static OffsetDateTime toOffset(LocalDateTime time) {
        if (time == null) return null;
        return time.atZone(ZONE).toOffsetDateTime();
    }
}
