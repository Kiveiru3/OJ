package com.academic.oj.service;

import com.academic.oj.entity.Submission;

public interface JudgeService {
    Submission judge(Submission submission);
}

