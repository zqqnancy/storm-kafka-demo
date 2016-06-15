package com.ks.bolt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import ring.middleware.cookies__init;

import com.ks.utils.Constant;
import com.ks.utils.JDBCUtil;
import com.ks.utils.LockCuratorSrc;
import com.ks.utils.MemcacheUtil;
import com.netflix.curator.framework.recipes.locks.InterProcessMutex;

import net.rubyeye.xmemcached.MemcachedClient;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;

public class SaveMysqlBolt extends BaseBasicBolt {

	private static final long serialVersionUID = -9135266042594721255L;
	private static Map<String, String> memberMap= null; //sendpay,counterMember
	private static MemcachedClient memClient = null;

	private static List<String> cacheList = null;
	private static boolean isOpen = true;

	@Override
	public void execute(Tuple tuple, BasicOutputCollector collector) {
		List<Object> list= tuple.getValues();
		
		String id = (String) list.get(0);
		String memberid = (String) list.get(1);
		String totalprice = (String) list.get(2);
		String youhui = (String) list.get(3);
		String sendpay = (String) list.get(4);
		
		saveCounterMember(memberid,sendpay,totalprice,youhui);//记录独立用户数
		

	}
	
	private void saveCounterMember(String memberid,String sendpay,String totalprice,String youhui){
		try{
			String key = sendpay+"_"+memberid;
			String vx = memClient.get(key);
			boolean isHasMem = false;
			if(StringUtils.isNotEmpty(vx)){
				isHasMem = true;
			}
			
			saveMap(sendpay,isHasMem,totalprice,youhui);
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void saveMap(String sendpay,boolean isHasMem,String totalprice,String youhui){
		if(isOpen){
			if(checkMap()){
				String value = memberMap.get(sendpay);//value = count(id),sum(totalPrice),sum(totalPrice - youhui),count(distinct memberid)
				if(value!=null){
					String[] vals = value.split(",");
					int id_num = Integer.valueOf(vals[0])+1;
					double tp = Double.valueOf(vals[1])+Double.valueOf(totalprice);
					double etp = Double.valueOf(vals[2])+(Double.valueOf(totalprice)-Double.valueOf(youhui));
					int counter_member = Integer.valueOf(vals[3])+(isHasMem?0:1);
					value =  id_num+","+tp+","+etp+","+counter_member;
				}else{
					value =  1+","+totalprice+","+(Double.valueOf(totalprice)-Double.valueOf(youhui))+","+(isHasMem?0:1);
				}
				System.out.println("sendpay = "+sendpay +"  value = "+value);
				memberMap.put(sendpay,value);
			}
		}else{
			String value = sendpay+"_"+1+","+totalprice+","+(Double.valueOf(totalprice)-Double.valueOf(youhui))+","+(isHasMem?0:1);
			cacheList.add(value);
		}
	}
	
	private boolean checkMap(){
		if(memberMap == null){
			memberMap = new HashMap<String, String>();
		}
		return true;
	}
	
	@Override
	public void prepare(Map stormConf, TopologyContext context) {
		memberMap = new HashMap<String, String>();
		memClient = MemcacheUtil.getInstance();
		
		cacheList = new ArrayList<String>();
		isOpen = true;
		
		Timer timer = new Timer();
		timer.schedule(new SaveMysqlBolt.cacheTimer(), new Date(), 5000);
		
	}

	
	class cacheTimer extends TimerTask{

		@Override
		public void run() {
			isOpen = false;
			Map<String, String> tmpMap = new HashMap<String, String>();
			tmpMap.putAll(memberMap);
			memberMap = new HashMap<String, String>();
			
			putDataMap();
			isOpen = true;
			
			saveMysql(tmpMap);
			
		}
		
	}
	
	private void putDataMap(){
		if(checkMap()){
			List<String> tmpList = new ArrayList<String>();
			copyList(tmpList);
			for(String str:tmpList){
				String[] arr = str.split("_");
				if(arr.length==2){
					String sendpay = arr[0];
					String old = arr[1];
					String[] olds = old.split(",");
					int nums = Integer.valueOf(olds[0]);
					String totalprice = olds[1];
					String youhui = olds[2];
					int cm = Integer.valueOf(olds[3]);
					
					String value = memberMap.get(sendpay);
					if(value!=null){
						String[] vals = value.split(",");
						int id_num = Integer.valueOf(vals[0])+nums;
						double tp = Double.valueOf(vals[1])+Double.valueOf(totalprice);
						double etp = Double.valueOf(vals[2])+(Double.valueOf(youhui));
						int counter_member = Integer.valueOf(vals[3])+cm;
						
						value =  id_num+","+tp+","+etp+","+counter_member;
					}else{
						value =  old;
					}
					
					System.out.println("sendpay = "+sendpay +"  value = "+value);
					
					memberMap.put(sendpay,value);
				}
			}
		}
	}
	
	private void copyList(List<String> tmpList){
		int num = cacheList.size();
		for(int i=0;i<num;i++){
			tmpList.add(cacheList.get(i));
		}
		for(int i=0;i<num;i++){
			cacheList.remove(0);
		}
	}
	
	private void saveMysql(Map<String, String> tmpMap){
		
		Connection conn = JDBCUtil.getConnectionByJDBC();
		InterProcessMutex lock = new InterProcessMutex(LockCuratorSrc.getCF(), Constant.LOCKS_ORDER);
		try{
			while (lock.acquire(10, TimeUnit.MINUTES) ) 
			{
				for(Map.Entry<String, String> entry:tmpMap.entrySet()){
					//id,order_nums,p_total_price,y_total_price,order_members,sendpay
					String key = entry.getKey();
					String value = entry.getValue();
					String[] vals = value.split(",");
					int id_num = Integer.valueOf(vals[0]);
					double tp = Double.valueOf(vals[1]);
					double etp = Double.valueOf(vals[2]);
					int counter_member = Integer.valueOf(vals[3]);
					
					Statement stmt= conn.createStatement();
					
					//select ...
					String sql = "select id,order_nums,p_total_price,y_total_price,order_members from total_order where sendpay='"+key+"'";
					
					ResultSet set = stmt.executeQuery(sql);
					
					int id = 0;
					int order_nums = 0;
					double p_total_price = 0;
					double y_total_price = 0;
					int order_member = 0;
					
					while(set.next()){
						 id = set.getInt(1);
						 order_nums = set.getInt(2);
						 p_total_price = set.getDouble(3);
						 y_total_price = set.getDouble(4);
						 order_member = set.getInt(5);
					}
					
					order_nums += id_num;
					p_total_price += tp;
					y_total_price += etp;
					order_member += counter_member;
					
					StringBuffer sBuffer = new StringBuffer();
					if(id==0){//insert
						sBuffer.append("insert into total_order(order_nums,p_total_price,y_total_price,order_members,sendpay) values(")
						.append(order_nums+","+p_total_price+","+y_total_price+","+order_member+",'"+key+"')");
					}else{//update
						sBuffer.append("update total_order set order_nums="+order_nums)
						.append(",p_total_price="+p_total_price)
						.append(",y_total_price="+y_total_price)
						.append(",order_members="+order_member)
						.append(" where id="+id);
						
					}
					
					System.out.println("sql = "+sBuffer.toString());
					
					stmt= conn.createStatement();
					stmt.executeUpdate(sBuffer.toString());
					conn.commit();
					
					stmt.close();
					
				}
				break;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(conn!=null){
					conn.close();
				}
				
				lock.release();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		

	}

}
