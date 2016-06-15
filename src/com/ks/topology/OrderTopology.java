package com.ks.topology;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;

import com.google.common.collect.ImmutableList;
import com.ks.bolt.CheckOrderBolt;
import com.ks.bolt.CounterBolt;
import com.ks.bolt.SaveMysqlBolt;
import com.ks.bolt.TranslateBolt;

public class OrderTopology {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			String kafkaZookeeper = "x00:2181,x01:2181,x02:2181";
			BrokerHosts brokerHosts = new ZkHosts(kafkaZookeeper);
			SpoutConfig kafkaConfig = new SpoutConfig(brokerHosts, "order", "/order", "id");
	        kafkaConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
	        kafkaConfig.zkServers =  ImmutableList.of("x00","x01","x02");
	        kafkaConfig.zkPort = 2181;
			
	        //kafkaConfig.forceFromStart = true;
			
	        TopologyBuilder builder = new TopologyBuilder();
	        builder.setSpout("spout", new KafkaSpout(kafkaConfig), 2);
	        builder.setBolt("check", new CheckOrderBolt(),1).shuffleGrouping("spout");
	        builder.setBolt("translate", new TranslateBolt(),3).shuffleGrouping("check");
	        builder.setBolt("save", new SaveMysqlBolt(),3).shuffleGrouping("translate");
	        
	        
	        Config config = new Config();
	        config.setDebug(true);
	        
	        if(args!=null && args.length > 0) {
	            config.setNumWorkers(2);
	            
	            StormSubmitter.submitTopology(args[0], config, builder.createTopology());
	        } else {        
	            config.setMaxTaskParallelism(3);
	
	            LocalCluster cluster = new LocalCluster();
	            cluster.submitTopology("special-topology", config, builder.createTopology());
	        
	            Thread.sleep(500000);
	
	            cluster.shutdown();
	        }
		}catch (Exception e) {
			e.printStackTrace();
		}

	}

}
