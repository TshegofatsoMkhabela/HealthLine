package com.healthline.backend.dispatch;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Broadcasts a dispatch's current state to /topic/dispatch/{dispatchId} on each transition. */
@Component
class DispatchNotifier {

  private final SimpMessagingTemplate messagingTemplate;

  DispatchNotifier(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  void notify(Dispatch dispatch) {
    messagingTemplate.convertAndSend(
        "/topic/dispatch/" + dispatch.getDispatchId(), DispatchResponseMapper.toBody(dispatch));
  }
}
