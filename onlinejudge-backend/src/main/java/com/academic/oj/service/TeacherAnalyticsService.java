package com.academic.oj.service;

import com.academic.oj.dto.TeacherAnalyticsVO;

public interface TeacherAnalyticsService {
    TeacherAnalyticsVO getOverview(Integer recentDays);
}
