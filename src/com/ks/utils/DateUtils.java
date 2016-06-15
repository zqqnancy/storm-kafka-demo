package com.ks.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	
	public static final String C_DATE_PATTON_DEFAULT = "yyyy-MM-dd";
	
	public static boolean isDate(String createDate,String startDate){
		try{
			SimpleDateFormat format = new SimpleDateFormat(C_DATE_PATTON_DEFAULT);
			Date cdate = format.parse(createDate);
			Date sdate = format.parse(startDate);
			
			if(cdate.getTime()>=sdate.getTime()){
				return true;
			}else{
				return false;
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
