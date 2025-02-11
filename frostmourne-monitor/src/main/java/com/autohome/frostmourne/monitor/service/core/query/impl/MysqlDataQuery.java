package com.autohome.frostmourne.monitor.service.core.query.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.autohome.frostmourne.monitor.dao.jdbc.IJdbcDao;
import com.autohome.frostmourne.monitor.model.contract.MetricContract;
import com.autohome.frostmourne.monitor.service.core.domain.MetricData;
import com.autohome.frostmourne.monitor.service.core.query.IMysqlDataQuery;

@Service
public class MysqlDataQuery implements IMysqlDataQuery {

    @Autowired
    protected IJdbcDao jdbcDao;

    @Override
    public MetricData queryMetricValue(DateTime start, DateTime end, MetricContract metricContract) {
        String timeField = metricContract.getDataNameContract().getTimestampField();
        if (StringUtils.isEmpty(metricContract.getDataNameContract().getTimestampField())) {
            throw new RuntimeException("数据名时间字段不能为空: " + metricContract.getDataNameContract().getDataName());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(metricContract.getQueryString());
        List<Object> argList = new ArrayList<>();
        if (!metricContract.getQueryString().toLowerCase().contains("where")) {
            sb.append(" where 1=1");
        }
        if (start != null) {
            sb.append(" and ").append(timeField).append(">=?");
            argList.add(this.formatDateParam(start));
        }
        if (end != null) {
            sb.append(" and ").append(timeField).append("<?");
            argList.add(this.formatDateParam(end));
        }
        String sql = sb.toString();
        Object[] args = argList.toArray();

        MetricData result = new MetricData();
        // 汇总
        long collectResult = this.collectResult(metricContract, sql, args);
        result.setMetricValue(collectResult);

        // 查询第一条
        if (collectResult > 0L) {
            Map<String, Object> lastDocument = this.queryLatestDocument(metricContract, sql, args);
            result.setLatestDocument(lastDocument);
        }
        return result;
    }

    @Override
    public MetricData querySql(MetricContract metricContract) {
        MetricData result = new MetricData();
        long count = collectResult(metricContract, metricContract.getQueryString(), null);
        result.setMetricValue(count);
        String querySql = "select * from ("+ metricContract.getQueryString() + ") t limit 1";
        List<Map<String, Object>> collectResult = jdbcDao.query(metricContract.getDataNameContract(),
                metricContract.getDataSourceContract(), querySql, null);
        if(collectResult != null && collectResult.size() > 0) {
            Map<String, Object> lastDocument = collectResult.get(0);
            result.setLatestDocument(lastDocument);
        }
        return result;
    }

    /**
     * 格式化日期类型参数
     *
     * @param date 日期参数
     * @return 格式化后的参数
     */
    protected Object formatDateParam(DateTime date) {
        return date.toDate();
    }

    protected long collectResult(MetricContract metricContract, String sql, Object[] args) {
        String collectSql = "select count(*) cnt from (" + sql + ") t";
        List<Map<String, Object>> collectResult = jdbcDao.query(metricContract.getDataNameContract(), metricContract.getDataSourceContract(), collectSql, args);
        if (collectResult != null && collectResult.size() > 0) {
            return ((Number)collectResult.get(0).get("cnt")).longValue();
        }
        return 0L;
    }

    protected Map<String, Object> queryLatestDocument(MetricContract metricContract, String sql, Object[] args) {
        String querySql = sql + " order by " + metricContract.getDataNameContract().getTimestampField() + " desc limit 1";
        List<Map<String, Object>> collectResult = jdbcDao.query(metricContract.getDataNameContract(), metricContract.getDataSourceContract(), querySql, args);
        if (collectResult != null && collectResult.size() > 0) {
            return collectResult.get(0);
        }
        return null;
    }

}
