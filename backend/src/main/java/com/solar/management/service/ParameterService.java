package com.solar.management.service;

import com.solar.management.entity.Parameter;
import com.solar.management.repository.ParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParameterService {

    private final ParameterRepository parameterRepository;

    public Parameter createParameter(Parameter parameter) {
        log.info("Creating new parameter: {}", parameter.getParameterKey());
        return parameterRepository.save(parameter);
    }

    public Parameter updateParameter(Long id, Parameter parameterDetails) {
        Parameter parameter = getParameterById(id);
        parameter.setParameterValue(parameterDetails.getParameterValue());
        parameter.setDescription(parameterDetails.getDescription());
        log.info("Updated parameter: {}", parameter.getParameterKey());
        return parameterRepository.save(parameter);
    }

    public Parameter getParameterById(Long id) {
        return parameterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parameter not found with id: " + id));
    }

    public Parameter getParameterByKey(String key) {
        return parameterRepository.findByParameterKey(key)
                .orElseThrow(() -> new RuntimeException("Parameter not found with key: " + key));
    }

    public List<Parameter> getAllParameters() {
        return parameterRepository.findAll();
    }

    public void deleteParameter(Long id) {
        Parameter parameter = getParameterById(id);
        log.info("Deleting parameter: {}", parameter.getParameterKey());
        parameterRepository.delete(parameter);
    }

    // Helper method to get hourly rate
    public BigDecimal getHourlyRate() {
        try {
            Parameter parameter = getParameterByKey("HOURLY_RATE");
            return parameter.getValueAsDecimal();
        } catch (Exception e) {
            log.warn("HOURLY_RATE parameter not found, using default: 35.00");
            return new BigDecimal("35.00");
        }
    }

    // Helper methods for company information
    public String getCompanyName() {
        try {
            return getParameterByKey("COMPANY_NAME").getParameterValue();
        } catch (Exception e) {
            log.warn("COMPANY_NAME parameter not found, using default");
            return "Nelvin Electrical";
        }
    }

    public String getCompanyAddress() {
        try {
            return getParameterByKey("COMPANY_ADDRESS").getParameterValue();
        } catch (Exception e) {
            log.warn("COMPANY_ADDRESS parameter not found, using default");
            return "Seaford Height SA 5169";
        }
    }

    public String getCompanyEmail() {
        try {
            return getParameterByKey("COMPANY_EMAIL").getParameterValue();
        } catch (Exception e) {
            log.warn("COMPANY_EMAIL parameter not found, using default");
            return "admin@nelvinelectrical.co.au";
        }
    }

    public String getCompanyPhone() {
        try {
            return getParameterByKey("COMPANY_PHONE").getParameterValue();
        } catch (Exception e) {
            log.warn("COMPANY_PHONE parameter not found, using default");
            return "+61 450 120 602";
        }
    }
}
