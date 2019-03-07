package com.gigya.android.sdk.model.profile;

import android.support.annotation.Nullable;

public class Skill {

    @Nullable
    private String level;
    @Nullable
    private String skill;
    private long years;

    @Nullable
    public String getLevel() {
        return level;
    }

    public void setLevel(@Nullable String level) {
        this.level = level;
    }

    @Nullable
    public String getSkill() {
        return skill;
    }

    public void setSkill(@Nullable String skill) {
        this.skill = skill;
    }

    public long getYears() {
        return years;
    }

    public void setYears(long years) {
        this.years = years;
    }
}