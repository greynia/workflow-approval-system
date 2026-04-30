package com.eva.workflow.approval.application.aireview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.eva.workflow.approval.application.request.event.LeaveRequestCreatedEvent;
import com.eva.workflow.approval.application.request.event.LeaveRequestCreatedHandler;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiReviewEventListener implements LeaveRequestCreatedHandler {

    private static final Logger log = LoggerFactory.getLogger(AiReviewEventListener.class);

    private final AiReviewOrchestrator orchestrator;

    @Override
    public void handle(LeaveRequestCreatedEvent event) {
        log.info("AI review triggered for leaveRequestId={}", event.leaveRequestId());
        orchestrator.review(event.leaveRequestId(), event.locale());
    }
}
