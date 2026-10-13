package com.samagan.smartlead.lead;

import com.samagan.smartlead.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LeadQualificationListener {

    private static final Logger log = LoggerFactory.getLogger(LeadQualificationListener.class);

    private final LeadQualificationService qualificationService;
    private final LeadResultRecorder resultRecorder;

    public LeadQualificationListener(LeadQualificationService qualificationService,
                                     LeadResultRecorder resultRecorder) {
        this.qualificationService = qualificationService;
        this.resultRecorder = resultRecorder;
    }

    // An exception here triggers a retry; once retries are exhausted the message is dead-lettered
    @RabbitListener(queues = RabbitConfig.QUALIFICATION_QUEUE)
    public void onQualificationRequested(LeadQualificationRequest request) {
        qualificationService.qualify(request.messageId());
    }

    @RabbitListener(queues = RabbitConfig.DEAD_LETTER_QUEUE)
    public void onQualificationDeadLettered(LeadQualificationRequest request) {
        log.error("Qualification gave up after retries for messageId={}", request.messageId());
        resultRecorder.markFailed(request.messageId());
    }
}
