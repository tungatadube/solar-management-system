package com.solar.management.controller;

import com.solar.management.entity.Parameter;
import com.solar.management.service.ParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/parameters")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParameterController {

    private final ParameterService parameterService;

    @PostMapping
    public ResponseEntity<Parameter> createParameter(@RequestBody Parameter parameter) {
        Parameter createdParameter = parameterService.createParameter(parameter);
        return new ResponseEntity<>(createdParameter, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Parameter>> getAllParameters() {
        List<Parameter> parameters = parameterService.getAllParameters();
        return ResponseEntity.ok(parameters);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parameter> getParameterById(@PathVariable Long id) {
        Parameter parameter = parameterService.getParameterById(id);
        return ResponseEntity.ok(parameter);
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<Parameter> getParameterByKey(@PathVariable String key) {
        Parameter parameter = parameterService.getParameterByKey(key);
        return ResponseEntity.ok(parameter);
    }

    @GetMapping("/hourly-rate")
    public ResponseEntity<BigDecimal> getHourlyRate() {
        BigDecimal hourlyRate = parameterService.getHourlyRate();
        return ResponseEntity.ok(hourlyRate);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Parameter> updateParameter(@PathVariable Long id, @RequestBody Parameter parameter) {
        Parameter updatedParameter = parameterService.updateParameter(id, parameter);
        return ResponseEntity.ok(updatedParameter);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParameter(@PathVariable Long id) {
        parameterService.deleteParameter(id);
        return ResponseEntity.noContent().build();
    }
}
