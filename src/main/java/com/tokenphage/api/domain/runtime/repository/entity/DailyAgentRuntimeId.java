package com.tokenphage.api.domain.runtime.repository.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** daily_agent_runtime 복합키(@IdClass) 클래스. */
public class DailyAgentRuntimeId implements Serializable {
    private Long githubId;
    private UUID deviceId;
    private LocalDate usageDate;
    private String source;
    private String model;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DailyAgentRuntimeId that)) {
            return false;
        }
        return Objects.equals(githubId, that.githubId) &&
               Objects.equals(deviceId, that.deviceId) &&
               Objects.equals(usageDate, that.usageDate) &&
               Objects.equals(source, that.source) &&
               Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubId, deviceId, usageDate, source, model);
    }
}
