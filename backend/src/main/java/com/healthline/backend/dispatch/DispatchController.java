package com.healthline.backend.dispatch;

import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency")
class DispatchController {

  private final DispatchService dispatchService;

  DispatchController(DispatchService dispatchService) {
    this.dispatchService = dispatchService;
  }

  @PostMapping("/trigger")
  ResponseEntity<Map<String, Object>> trigger(@RequestBody TriggerRequest request) {
    if (request.triagePayload() == null || !request.triagePayload().hasValidBloodType()) {
      return ResponseEntity.badRequest().build();
    }
    Dispatch dispatch = dispatchService.trigger(request.triagePayload(), request.location());
    URI location = URI.create("/api/emergency/" + dispatch.getDispatchId());
    return ResponseEntity.created(location).body(DispatchResponseMapper.toBody(dispatch));
  }

  @GetMapping("/{dispatchId}")
  ResponseEntity<Map<String, Object>> get(@PathVariable String dispatchId) {
    return dispatchService
        .get(dispatchId)
        .map(dispatch -> ResponseEntity.ok(DispatchResponseMapper.toBody(dispatch)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/{dispatchId}/cancel")
  ResponseEntity<Map<String, Object>> cancel(@PathVariable String dispatchId) {
    return dispatchService
        .cancel(dispatchId)
        .map(dispatch -> ResponseEntity.ok(DispatchResponseMapper.toBody(dispatch)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
