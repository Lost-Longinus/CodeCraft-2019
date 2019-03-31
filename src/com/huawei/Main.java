package com.huawei;

import com.huawei.dao.GraphInfo;
import com.huawei.dao.InfoFromTxt;
import com.huawei.model.*;
import com.huawei.service.Dijkstra;
import com.huawei.service.Dispatch;

import java.io.*;
import java.util.*;

public class Main {
    /**
     * 主程序入口
     * @param args          输入输出文件路径
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        long pastTime = System.currentTimeMillis();
        /*String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String answerPath = args[3];*/
		String carPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\car.txt";
        String roadPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\road.txt";
        String crossPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\cross.txt";
        String answerPath = "D:\\Github\\CodeCraft\\src\\com\\huawei\\view\\answer.txt";

        Map<Integer, Car> carIdMap = InfoFromTxt.getIdMap(carPath, Car.class);
        Map<Integer, Road> roadIdMap = InfoFromTxt.getIdMap(roadPath, Road.class);
        Map<Integer, Cross> crossIdMap = InfoFromTxt.getIdMap(crossPath, Cross.class);

        GraphInfo graphInfo = new GraphInfo(carIdMap, roadIdMap, crossIdMap);
        int startTime = 1;
        List<Integer> deadCarIds;
        int arrivedCount = Dispatch.statusCarCount(graphInfo.getCarIdMap(), CarStatus.Arrived);
        while (arrivedCount < graphInfo.getCarIdMap().size()){
            Dispatch.delivery(graphInfo, startTime);
            System.out.println("arrivedCount = "+arrivedCount);
            boolean flag = true;//用于记录死锁
            statusReset(graphInfo);
            while (flag){
                //System.out.println("innerloop----------------------innerloop");
                GraphInfo tempGraphInfo = graphInfo;
                //实时调度路口附近需要决定方向的车辆
                deadCarIds = Dispatch.dispatchCarOnRoad(tempGraphInfo);
                if (deadCarIds.size() == 0){
                    flag = false;
                }
                //陷入死锁，解开
                else {
                    for (Integer carId : deadCarIds) {
                        if (carId != 0){
                            tempGraphInfo.getCarIdMap().get(carId).setNextCrossDirection(Direction.D);
                        }
                    }
                }
                graphInfo = tempGraphInfo;
            }
            arrivedCount = Dispatch.statusCarCount(graphInfo.getCarIdMap(), CarStatus.Arrived);
            System.out.println("startTime = "+startTime);
            System.out.println("计算用时： "+(System.currentTimeMillis() - pastTime));
            startTime++;
        }
        //写出结果
        ArrayList<ArrayList<Integer>> answerLists = new ArrayList<>();
        for(Integer carId:graphInfo.getCarIdList()){
            ArrayList<Integer> answerList = new ArrayList<>();
            answerList.add(carId);
            answerList.add(carIdMap.get(carId).getStartTime());
            List<Integer> path_No = graphInfo.getCarIdMap().get(carId).getPath();
            for (int i = 0; i < path_No.size() - 2; i++) {
                if (graphInfo.getRoadStartEndMap().get(path_No.get(i) + "_" + path_No.get(i + 1)) != null) {
                    answerList.add(graphInfo.getRoadStartEndMap().get(path_No.get(i) + "_" + path_No.get(i + 1)).getRoadId());
                } else {
                    answerList.add(graphInfo.getRoadStartEndMap().get(path_No.get(i + 1) + "_" + path_No.get(i)).getRoadId());
                }
            }
        }
        writeAnswer(answerPath, answerLists);
    }

    /**
     * 重置地图中车辆状态，将stop变更为on_road，为下一时间片调度做准备
     * @param graphInfo     地图信息
     */
    private static void statusReset(GraphInfo graphInfo){
        for(Integer carId:graphInfo.getCarIdList()){
            if (graphInfo.getCarIdMap().get(carId).getStatus() == CarStatus.Stop){
                graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.On_Road);
            }
        }
    }

    /**
     * 向answer写出结果
     * @param answerPath        结果文件路径
     * @param answerLists       结果集
     * @throws IOException
     */
    public static void writeAnswer(String answerPath, ArrayList<ArrayList<Integer>> answerLists) throws IOException {
        OutputStream outputStream = new FileOutputStream(new File(answerPath));
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        for(List list:answerLists){
            bufferedWriter.append("(");
            for (int i = 0; i < list.size(); i++) {
                if (i != list.size() - 1){
                    bufferedWriter.append(list.get(i) + ", ");
                }
                else{
                    bufferedWriter.append(list.get(i).toString());
                }
            }
            bufferedWriter.append(")\n");
        }
        bufferedWriter.flush();
        bufferedWriter.close();
        outputStreamWriter.close();
        outputStream.close();
    }
}
