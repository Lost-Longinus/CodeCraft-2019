package com.huawei;

import com.huawei.dao.GraphInfoForAns;
import com.huawei.dao.InfoFromTxt;
import com.huawei.model.Car;
import com.huawei.model.CarStatus;
import com.huawei.model.Cross;
import com.huawei.model.Road;
import com.huawei.service.DispatchForAnswer;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Pengfei Jin on 2019/3/23.
 */
public class MainForDispatch {
	public static void main(String[] args) throws Exception {
		String carPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\car.txt";
		String roadPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\road.txt";
		String crossPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\cross.txt";
		String answerPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\answer.txt";

		Map<Integer, Car> carIdMap = InfoFromTxt.getIdMap(carPath, Car.class);
		Map<Integer, Road> roadIdMap = InfoFromTxt.getIdMap(roadPath, Road.class);
		Map<Integer, Cross> crossIdMap = InfoFromTxt.getIdMap(crossPath, Cross.class);
		ArrayList<ArrayList<Integer>> answerLists = InfoFromTxt.dataFromTxt(answerPath);

		GraphInfoForAns graphInfoForAns = new GraphInfoForAns(carIdMap, roadIdMap, crossIdMap, answerLists);
		int time = 2;
		int arrivedCount = 0;
		boolean flag = true;
		DispatchForAnswer.initDispatch(graphInfoForAns);//time=1
		statusReset(graphInfoForAns);
			while (arrivedCount < graphInfoForAns.getCarIdMap().size() && flag == true) {
			flag = DispatchForAnswer.dispatchCarOnRoad(graphInfoForAns);
			DispatchForAnswer.fromGarageToRoad(graphInfoForAns, time);
			//System.out.println("发车后On_Road = "+DispatchForAnswer.statusCarCount(graphInfoForAns.getCarIdMap(),CarStatus.On_Road));
			statusReset(graphInfoForAns);
			arrivedCount = getArrivedCount(graphInfoForAns);
			time++;
			//System.out.println(flag);
		}
		System.out.println(time);
	}

	public static int getArrivedCount(GraphInfoForAns graphInfoForAns){
		int count = 0;
		for(Integer carId: graphInfoForAns.getCarIdList()){
			if (graphInfoForAns.getCarIdMap().get(carId).getStatus() == CarStatus.Arrived){
				count++;
			}
		}
		return count;
	}

	private static void statusReset(GraphInfoForAns graphInfoForAns){
		for(Integer carId:graphInfoForAns.getCarIdList()){
			if (graphInfoForAns.getCarIdMap().get(carId).getStatus() == CarStatus.Stop){
				graphInfoForAns.getCarIdMap().get(carId).setStatus(CarStatus.On_Road);
			}
		}
	}
}
