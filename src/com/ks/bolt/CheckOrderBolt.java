package com.ks.bolt;

import org.apache.commons.lang.StringUtils;
import org.jboss.netty.util.internal.StringUtil;

import com.ks.utils.DateUtils;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class CheckOrderBolt extends BaseBasicBolt {

	private static final long serialVersionUID = 689316581965807841L;

	@Override
	public void execute(Tuple tuple, BasicOutputCollector collector) {
		String data = tuple.getString(0);
		//订单号       用户id       原金额        优惠价     标示字段    下单时间
		//id           memberid     totalprice    youhui     sendpay     createdate 
		if(data!=null && data.length()>0){
			String[] values = data.split("\t");
			if(values.length==6){
				String id = values[0];
				String memberid = values[1];
				String totalprice = values[2];
				String youhui = values[3];
				String sendpay = values[4];
				String createdate = values[5];
				
				if(StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(memberid) && StringUtils.isNotEmpty(totalprice)){
					if(DateUtils.isDate(createdate, "2014-04-19")){
						collector.emit(new Values(id,memberid,totalprice,youhui,sendpay));
					}
				}
			}
		}

	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("id","memberid","totalprice","youhui","sendpay"));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String data = "4264782	41157	331	42	2	2014-04-20";
		if(data!=null && data.length()>0){
			String[] values = data.split("\t");
			if(values.length==6){
				String id = values[0];
				String memberid = values[1];
				String totalprice = values[2];
				String youhui = values[3];
				String sendpay = values[4];
				String createdate = values[5];
				
				if(StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(memberid) && StringUtils.isNotEmpty(totalprice)){
					if(DateUtils.isDate(createdate, "2014-04-19")){
						System.out.println("   true");
					}else{
						System.out.println("   false");
					}
				}
			}
		}
	}

}
