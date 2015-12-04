package ru.taaasty.rest.model;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 03.12.15.
 */
public class AppVersionResponse {

    /**
     * Переданная версия серверу неизвестна
     */
    public static final String UPDATE_STATUS_UNKNOWN = "unknown";

    /**
     * Обновление не требуется
     */
    public static final String UPDATE_STATUS_NOT_REQUIRED = "not_required";

    /**
     * рекомендуется, необязательный апдейт
     */
    public static final String UPDATE_STATUS_RECOMMENDED = "recommended";

    /**
     * требуется, обязательный апдейт
     */
    public static final String UPDATE_STATUS_REQUIRED = "required";

    public String minimalSupportedVersion;

    public String minimalRecommendedVersion;

    /**
     * Статус
     */
    public String update;

    @Nullable
    public String message;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppVersionResponse that = (AppVersionResponse) o;

        if (minimalSupportedVersion != null ? !minimalSupportedVersion.equals(that.minimalSupportedVersion) : that.minimalSupportedVersion != null)
            return false;
        if (minimalRecommendedVersion != null ? !minimalRecommendedVersion.equals(that.minimalRecommendedVersion) : that.minimalRecommendedVersion != null)
            return false;
        if (update != null ? !update.equals(that.update) : that.update != null) return false;
        return !(message != null ? !message.equals(that.message) : that.message != null);

    }

    @Override
    public int hashCode() {
        int result = minimalSupportedVersion != null ? minimalSupportedVersion.hashCode() : 0;
        result = 31 * result + (minimalRecommendedVersion != null ? minimalRecommendedVersion.hashCode() : 0);
        result = 31 * result + (update != null ? update.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AppVersionResponse{" +
                "message='" + message + '\'' +
                ", minimalSupportedVersion='" + minimalSupportedVersion + '\'' +
                ", minimalRecommendedVersion='" + minimalRecommendedVersion + '\'' +
                ", update='" + update + '\'' +
                '}';
    }
}
