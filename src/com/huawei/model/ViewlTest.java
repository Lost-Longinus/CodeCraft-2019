package com.huawei.model;

import com.huawei.dao.GraphInfo;
import com.huawei.dao.InfoFromTxt;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class ViewlTest {
	public static void main(String[] args) throws Exception {
		String carPath = "C:\\Users\\金鹏飞\\Desktop\\maps\\1-map-exam-1\\car.txt";
		String roadPath = "C:\\Users\\金鹏飞\\Desktop\\maps\\1-map-exam-1\\road.txt";
		String crossPath = "C:\\Users\\金鹏飞\\Desktop\\maps\\1-map-exam-1\\cross.txt";
		Map<Integer, Car> carIdMap = InfoFromTxt.getIdMap(carPath, Car.class);
		Map<Integer, Road> roadIdMap = InfoFromTxt.getIdMap(roadPath, Road.class);
		Map<Integer, Cross> crossIdMap = InfoFromTxt.getIdMap(crossPath, Cross.class);

		GraphInfo graphInfo = new GraphInfo(carIdMap, roadIdMap, crossIdMap);

		Map<Integer, Integer> timeCarCount = new HashMap<>();
		Map<Integer, Integer> crossCarCount = new HashMap<>();
		for (Integer carId : carIdMap.keySet()) {
			if (timeCarCount.get(graphInfo.getCarIdMap().get(carId).getStartTime()) == null){
				timeCarCount.put(graphInfo.getCarIdMap().get(carId).getStartTime(), 1);
			}else {
				timeCarCount.put(graphInfo.getCarIdMap().get(carId).getStartTime(), timeCarCount.get(graphInfo.getCarIdMap().get(carId).getStartTime()) + 1);
			}

			if (crossCarCount.get(graphInfo.getCarIdMap().get(carId).getDestination()) == null){
				crossCarCount.put(graphInfo.getCarIdMap().get(carId).getDestination(), 1);
			}else {
				crossCarCount.put(graphInfo.getCarIdMap().get(carId).getDestination(), crossCarCount.get(graphInfo.getCarIdMap().get(carId).getDestination()) + 1);
			}
		}

		System.out.println(timeCarCount);
		System.out.println("------------------------------------------------");
		int sum = 0;
		for (Integer crossId : crossCarCount.keySet()) {
			System.out.println(crossId + ":" +crossCarCount.get(crossId));
			sum += crossCarCount.get(crossId);
		}
		System.out.println("------------------------------------------------");
		System.out.println(sum);
	}
}
