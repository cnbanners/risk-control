package com.ai.risk.analysis.modules.collect.task;

import com.ai.risk.analysis.modules.collect.accumulator.hbase.OpCodeAccumulator;
import com.ai.risk.analysis.modules.collect.accumulator.hbase.ServiceAccumulator;
import com.ai.risk.analysis.modules.collect.entity.po.CallCountUnit;
import com.ai.risk.analysis.modules.warning.service.IOpcodeAccumulatorService;
import com.ai.risk.analysis.modules.warning.service.IServiceAccumulatorService;
import com.ai.risk.analysis.modules.warning.service.IWarningService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HBase聚合
 *
 * @author Steven
 * @date 2019-06-04
 */
@Component
public class HBaseSinkTask {

	@Autowired
	private IServiceAccumulatorService serviceAccumulatorService;

	@Autowired
	private IOpcodeAccumulatorService opcodeAccumulatorService;

	@Autowired
	private IWarningService warningService;

	@Autowired
	private ServiceAccumulator serviceAggregate;

	@Autowired
	private OpCodeAccumulator opCodeAggregate;

	@Scheduled(cron = "0 */5 * * * ?") // 每 5 分钟执行一次
	//@Scheduled(cron = "0 0 * * * ?") // 整点执行一次
	public void scheduled() {

		Map<String, AtomicLong> serviceLocalCounts = serviceAggregate.getLocalCounts();
		Map<String, AtomicLong> opCodeLocalCounts = opCodeAggregate.getLocalCounts();

		String now = DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmm");
		String rowNamePrefix = now + "-" ;

		List<CallCountUnit> serviceList = new ArrayList();
		List<CallCountUnit> opcodeList = new ArrayList();
		for (String svcName : serviceLocalCounts.keySet()) {
			long cnt = serviceLocalCounts.get(svcName).get();
			CallCountUnit unit = new CallCountUnit();
			unit.setName(svcName);
			unit.setCount(cnt);
			serviceList.add(unit);
		}
		serviceLocalCounts.clear();

		for (String opCode : opCodeLocalCounts.keySet()) {
			long cnt = opCodeLocalCounts.get(opCode).get();
			CallCountUnit unit = new CallCountUnit();
			unit.setName(opCode);
			unit.setCount(cnt);
			unit.setCount(cnt);
			opcodeList.add(unit);
		}
		opCodeLocalCounts.clear();

		// 开始预警分析
		serviceAccumulatorService.serviceAccumulator(now, serviceList);
		opcodeAccumulatorService.opcodeAccumulator(now, opcodeList);
		try {
			warningService.warning("service", now, serviceList);
			warningService.warning("opcode", now, opcodeList);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
