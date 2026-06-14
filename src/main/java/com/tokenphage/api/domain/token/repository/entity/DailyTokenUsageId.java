package com.tokenphage.api.domain.token.repository.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class DailyTokenUsageId implements Serializable {
    private Long githubId;
    private UUID deviceId;
    private LocalDate usageDate;
    private String model;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DailyTokenUsageId that)) {
            return false;
        }
        return Objects.equals(githubId, that.githubId) &&
               Objects.equals(deviceId, that.deviceId) &&
               Objects.equals(usageDate, that.usageDate) &&
               Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubId, deviceId, usageDate, model);
    }
}
