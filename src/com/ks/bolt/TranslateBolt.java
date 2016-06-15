package com.ks.bolt;

import java.util.List;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TranslateBolt extends BaseBasicBolt {
	/**
	 * 
	 */
	private static final long serialVersionUID = 214673588753475836L;

	@Override
	public void execute(Tuple tuple, BasicOutputCollector collector) {
		List<Object> list= tuple.getValues();
		
		String id = (String) list.get(0);
		String memberid = (String) list.get(1);
		String totalprice = (String) list.get(2);
		String youhui = (String) list.get(3);
		String sendpay = (String) list.get(4);
		
		if("0".equals(sendpay)){
			sendpay = "-1";
		}
		
		System.out.println("list="+list.toString()+"  sendpay = "+sendpay);
		
		collector.emit(new Values(id,memberid,totalprice,youhui,sendpay));

	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("id","memberid","totalprice","youhui","sendpay"));

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
