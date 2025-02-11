package com.autohome.frostmourne.monitor.service.core.rule;

import java.util.Map;

import com.autohome.frostmourne.monitor.model.contract.MetricContract;
import com.autohome.frostmourne.monitor.model.contract.RuleContract;
import com.autohome.frostmourne.monitor.service.core.execute.AlarmProcessLogger;
import com.autohome.frostmourne.monitor.service.core.metric.IMetric;
import com.autohome.frostmourne.monitor.service.core.template.ITemplateService;

public class NumericRule extends AbstractRule {

    public NumericRule(ITemplateService templateService) {
        super(templateService);
    }

    @Override
    public boolean verify(AlarmProcessLogger alarmProcessLogger, RuleContract ruleContract, MetricContract metricContract, IMetric metric) {
        Map<String, Object> context = context(alarmProcessLogger, ruleContract, metricContract, metric);
        Double threshold = findThreshold(ruleContract);
        Double number = findNumber(context);
        String operator = findOperation(ruleContract);

        if ("gte".equalsIgnoreCase(operator)) {
            return number >= threshold;
        }
        if ("lte".equalsIgnoreCase(operator)) {
            return number <= threshold;
        }
        if ("equal".equalsIgnoreCase(operator)) {
            return number.equals(threshold);
        }

        throw new RuntimeException("unknown operator: " + operator);
    }

    private Double findNumber(Map<String, Object> context) {
        return Double.parseDouble(context.get("NUMBER").toString());
    }

    private Double findThreshold(RuleContract ruleContract) {
        if (!ruleContract.getSettings().containsKey("THRESHOLD")) {
            throw new RuntimeException("NumericRule中THRESHOLD属性不存在。");
        }
        return Double.parseDouble(ruleContract.getSettings().get("THRESHOLD"));
    }

    private String findOperation(RuleContract ruleContract) {
        if (!ruleContract.getSettings().containsKey("OPERATOR")) {
            throw new RuntimeException("NumericRule中OPERATOR属性不存在。");
        }
        return ruleContract.getSettings().get("OPERATOR");
    }
}
