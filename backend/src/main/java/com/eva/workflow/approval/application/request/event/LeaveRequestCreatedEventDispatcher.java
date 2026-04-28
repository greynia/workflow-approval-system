package com.eva.workflow.approval.application.request.event;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeaveRequestCreatedEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestCreatedEventDispatcher.class);

    private final List<LeaveRequestCreatedHandler> handlers;

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveRequestCreated(LeaveRequestCreatedEvent event) {
        // Best-effort fan-out: dispatch the same event to all registered handlers,
        // isolate individual handler failures with logging, and do not fail the
        // original request after the transaction has already committed.
        for (LeaveRequestCreatedHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                log.error("Handler {} failed for leaveRequestId={}", handler.getClass().getSimpleName(), event.leaveRequestId(), e);
            }
        }
    }
}
