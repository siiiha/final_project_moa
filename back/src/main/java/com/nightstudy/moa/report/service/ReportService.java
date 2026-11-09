package com.nightstudy.moa.report.service;

import com.nightstudy.moa.report.dto.ReportCreateRequest;

public interface ReportService {
    Long createReport(Long reporterId, ReportCreateRequest request);
}
