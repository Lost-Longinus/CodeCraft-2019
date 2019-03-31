package com.huawei.service;

import com.huawei.comparator.CarComparator;
import com.huawei.dao.GraphInfo;
import com.huawei.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Pengfei Jin on 2019/3/25.
 */

public class Dispatch {

    /**
     * 该方法用于发送车库的车辆 ！！！注意，车辆出发时间会被修改，最后导出ans时要回输入信息获取startTime
     * @param graphInfo     包含地图上所有信息
     * @param startTime     发送当前时间出发的车辆
     */
    public static void delivery(GraphInfo graphInfo, int startTime){
        //先规划当前时刻出发车辆的路径，存入地图信息中
        for (Integer carId:graphInfo.getCarIdList()) {
            int countLimte = 10000;
            if (graphInfo.getCarIdMap().get(carId).getStartTime() == startTime){
                if (countLimte>=0){
                    //每次规划路径前，更新地图信息
                    graphInfo.updateNowCarGraphInfo(graphInfo.getCarIdMap().get(carId));
                    ArrayList<Integer> pathList = Dijkstra.getShortPath(graphInfo.getCarIdMap().get(carId).getOrigin(),
                            graphInfo.getCarIdMap().get(carId).getDestination(), graphInfo.getVertexIdMap(), graphInfo.getVertexEdgeListMap());
                    Collections.reverse(pathList);
                    graphInfo.getCrossPathCarIdMap().put(carId, pathList);
                    countLimte--;
                }else {
                    graphInfo.getCarIdMap().get(carId).setStartTime(graphInfo.getCarIdMap().get(carId).getStartTime()+1);
                }
            }
        }
        //发车
        for(Integer carId:graphInfo.getCarIdList()){
            if (graphInfo.getCarIdMap().get(carId).getStartTime() == startTime){
                String startEnd = graphInfo.getCrossPathCarIdMap().get(carId).get(0)+"_"+graphInfo.getCrossPathCarIdMap().get(carId).get(1);
                Position barrierPosition = getBarrierPosition(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId(), graphInfo);
                int speed = graphInfo.getEdgeIdMap().get(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId()).getVelocityLim()>graphInfo.getCarIdMap().get(carId).getCarVelocity()?
                        graphInfo.getCarIdMap().get(carId).getCarVelocity():graphInfo.getEdgeIdMap().get(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId()).getVelocityLim();
                //被阻塞无法上路，出发时间+1
                if (barrierPosition.disToEnd == graphInfo.getEdgeStartEndMap().get(startEnd).getLength()-1 &&
                        barrierPosition.lane_No == graphInfo.getEdgeStartEndMap().get(startEnd).getLane_No() - 1){
                    graphInfo.getCarIdMap().get(carId).setStartTime(graphInfo.getCarIdMap().get(carId).getStartTime()+1);
                }
                //可以上路
                else {
                    barrierPosition.disToEnd = barrierPosition.disToEnd >= graphInfo.getEdgeStartEndMap().get(startEnd).getLength() - speed?
                            barrierPosition.disToEnd + 1:graphInfo.getEdgeStartEndMap().get(startEnd).getLength() - speed;
                    graphInfo.getCarIdMap().get(carId).setPosition(barrierPosition);
                    graphInfo.getEdgeStartEndMap().get(startEnd).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));
                    graphInfo.getCarIdMap().get(carId).getPath().add(graphInfo.getCrossPathCarIdMap().get(carId).get(0));
                    graphInfo.getCarIdMap().get(carId).setNextCrossDirection(
                            setDirectionByCross(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1), carId, graphInfo));
                    graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                }
            }
        }
    }

    /**
     * ！！！！！使用该方法前保存graphInfo副本，只有返回列表为空时才能将副本赋值回去
     * 驱动道路上车辆，如果发生死锁返回不能stop车辆id列表，没有则返回list为空
     * @param graphInfo     地图信息
     * @return              返回陷入死锁车辆id列表，没有则返回list为空
     */
    public static List<Integer> dispatchCarOnRoad(GraphInfo graphInfo){
        //第一步：调度所有车，处理掉够驶停的车辆，否则标记为await，进入第二步调度
        for(Edge edge:graphInfo.getEdgeList()){
            /* 调整所有道路上在道路上的车辆，让道路上车辆前进，只要不出路口且可以到达终止状态的车辆
             * 分别标记出来等待的车辆（要出路口的车辆，或者因为要出路口的车辆阻挡而不能前进的车辆）
             * 和终止状态的车辆（在该车道内可以经过这一次调度可以行驶其最大可行驶距离的车辆）*/
            if (edge.getCarsOnThisEdge().size() != 0){
                //将当前道路车辆排序，从出路口往进路口依次调度
                Collections.sort(edge.getCarsOnThisEdge(), new CarComparator());
                for (int i = 0; i < edge.getCarsOnThisEdge().size(); i++) {
                    int speed = edge.getVelocityLim()>=edge.getCarsOnThisEdge().get(i).getCarVelocity()?
                            edge.getCarsOnThisEdge().get(i).getCarVelocity():edge.getVelocityLim();
                    int barrierToEnd = -1;
                    int barrierStatus = CarStatus.Await;
                    int carId = edge.getCarsOnThisEdge().get(i).getCarId();
                    //获取当前道路前方障碍位置和状态
                    for(Car car:graphInfo.getCarIdMap().get(carId).getPosition().edge.getCarsOnThisEdge()){
                        if (car.getPosition().lane_No == graphInfo.getCarIdMap().get(carId).getPosition().lane_No &&
                                car.getPosition().disToEnd < graphInfo.getCarIdMap().get(carId).getPosition().disToEnd){
                            barrierToEnd = barrierToEnd>car.getPosition().disToEnd?barrierToEnd:car.getPosition().disToEnd;
                            barrierStatus = car.getStatus();
                        }
                    }
                    //前方有障碍
                    if (barrierToEnd != -1){
                        //障碍影响正常行驶
                        if (graphInfo.getCarIdMap().get(carId).getPosition().disToEnd-speed <= barrierToEnd){
                            if (barrierStatus == CarStatus.Stop){
                                graphInfo.getCarIdMap().get(carId).getPosition().disToEnd = barrierToEnd + 1;
                                graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                            }else{
                                graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Await);
                            }
                        }
                        //障碍不影响正常行驶，全速驶停
                        else {
                            graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
                            graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                        }
                    }
                    //前方没有障碍
                    else{
                        if (graphInfo.getCarIdMap().get(carId).getPosition().disToEnd-speed <= barrierToEnd){
                            //该车到达目的地
                            graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Await);
                        }else{
                            graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
                            graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                        }
                    }
                }
            }
        }
        //第二步：：按路口id升序，调度路口车辆
        //用于记录陷入死锁车辆id
        List<Integer> deadLockCarIds = new ArrayList<>();
        //用于记录每次调度后等待状态车辆数量，如果两次调度未能降低该值，则陷入死循环，返回陷入死循环车辆id列表
        int totalAwaitCount = statusCarCount(graphInfo.getCarIdMap(), CarStatus.Await);
        while (totalAwaitCount != 0){
            crossLoop:for(Integer crossId:graphInfo.getCrossIdList()){
                int[] crossIds = graphInfo.getNeighbourCrossesIdMap().
                        get(crossId);
                //获取邻接道路id，没有路则记为0
                List<Integer> edgeIds = new ArrayList<>();
                for (int i = 0; i < crossIds.length; i++) {
                    if (crossIds[i] == 0){
                        edgeIds.add(0);//没有路则记为0
                    }else if (graphInfo.getEdgeStartEndMap().get(crossIds[i]+"_"+crossId)==null){
                        edgeIds.add(0);//没有路则记为0
                    }else {
                        edgeIds.add(graphInfo.getEdgeStartEndMap().get(crossIds[i]+"_"+crossId).getEdgeId());
                    }
                }
                //路口车辆按道路升序调度,故先排序
                for (int j = 0; j < edgeIds.size(); j++) {
                    if (edgeIds.get(j) != 0){
                        if (graphInfo.getEdgeIdMap().get(edgeIds.get(j)).getCarsOnThisEdge().size() != 0){
                            Collections.sort(graphInfo.getEdgeIdMap().get(edgeIds.get(j)).getCarsOnThisEdge(), new CarComparator());
                        }
                    }
                }
                //————————路口调度算法，核心！！！————————
                //用于记录循环一个路口邻接道路后，是否有车辆状态变化，有则进入本路口下一个循环，没有则跳出该路口，调度下一个路口
                String crossFlag = getAllAwaitCarsFlag(getPriorityCarIdsAtCross(crossId, graphInfo));
                //路口按id升序轮询
                while (!crossFlag.equals("0000")){
                    edgeLoop:for(Integer edgeId:edgeIds) {
                        if (edgeId != 0) {
                            if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size() != 0) {
                                for (int i = 0; i < graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size(); i++) {
                                    int priorityIndex = getFirstAwaitCarId(edgeId, graphInfo);
                                    if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().get(i).getCarId() == priorityIndex) {
                                        //获取路口所有第一优先级车辆id，用于判断冲突,false不冲突
                                        if (conflictDiagnose(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().get(i).getCarId(),
                                                getPriorityCarIdsAtCross(crossId, graphInfo), graphInfo) == false) {
                                            boolean flag = runAwaitCarAtCross(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().get(i).getCarId(), crossId, graphInfo);
                                            if (flag){
                                                i--;
                                                Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
                                            }
                                        } else {
                                            continue edgeLoop;
                                        }
                                    } else {
                                        continue;
                                    }
                                }
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                    int[] awaitCarIds = getPriorityCarIdsAtCross(crossId, graphInfo);
                    String  tempAllAwaitCarsFlag = getAllAwaitCarsFlag(awaitCarIds);
                    if (tempAllAwaitCarsFlag.equals(crossFlag)){
                        //存入当前路口死锁车辆id
                        for (int i = 0; i < awaitCarIds.length; i++) {
                            deadLockCarIds.add(awaitCarIds[i]);
                        }
                        continue crossLoop;
                    }else {
                        crossFlag = tempAllAwaitCarsFlag;
                    }
                }
            }
            //本次按路口升序调度结束，比较被调度车辆是否发生变化，没有则说明发生死锁，返回false
            int tempCount = statusCarCount(graphInfo.getCarIdMap(), CarStatus.Await);
            if (tempCount - totalAwaitCount == 0){
                return deadLockCarIds;
            }else{
                deadLockCarIds.clear();
                totalAwaitCount = tempCount;
            }
        }
        return deadLockCarIds;
    }

    /**
     * 获取将要驶入道路的障碍位置
     * @param edgeId        将要驶入道路id 
     * @param graphInfo     地图信息
     * @return              返回障碍位置
     */
    private static Position getBarrierPosition(Integer edgeId, GraphInfo graphInfo){
        int barrierToEnd = -1;
        Position barrierPosition = new Position();
        Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());//按优先级排列
        barrierPosition.edge = graphInfo.getEdgeIdMap().get(edgeId);
        if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size() != 0){
            barrierPosition.disToEnd = graphInfo.getEdgeIdMap().get(edgeId).getLength() - 1;
            barrierPosition.lane_No = graphInfo.getEdgeIdMap().get(edgeId).getLane_No() - 1;
            for (int i = 0; i < graphInfo.getEdgeIdMap().get(edgeId).getLane_No(); i++) {
                barrierToEnd = -1;
                for(Car car:graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge()){
                    if (car.getPosition().lane_No == i){
                        barrierToEnd = car.getPosition().disToEnd>barrierToEnd?car.getPosition().disToEnd:barrierToEnd;
                    }
                }
                if (barrierToEnd < graphInfo.getEdgeIdMap().get(edgeId).getLength() - 1){
                    barrierPosition.lane_No = i;
                    barrierPosition.disToEnd = barrierToEnd;
                    return barrierPosition;
                }
            }
        }else {
            barrierPosition.disToEnd = -1;
            barrierPosition.lane_No = 0;
        }
        return barrierPosition;
    }

    /**
     * 利用某辆车身后路口id获取车前路口行驶方向
     * @param crossId       车身后路口id
     * @param carId         车辆id
     * @param graphInfo     地图信息
     * @return              方向Direction
     */
    public static int setDirectionByCross(int crossId, int carId, GraphInfo graphInfo){
        int backCrossIndex = graphInfo.getCrossPathCarIdMap().get(carId).lastIndexOf(crossId);
        int frontCrossId = graphInfo.getCrossPathCarIdMap().get(carId).get(backCrossIndex+1);
        int index03 = 100;
        int[] crosses = graphInfo.getNeighbourCrossesIdMap().get(frontCrossId);
        if (graphInfo.getCrossPathCarIdMap().get(carId).size() - backCrossIndex - 2 > 0){
            int nextCrossId = graphInfo.getCrossPathCarIdMap().get(carId).get(backCrossIndex+2);
            for (int i = 0; i < crosses.length; i++) {
                if (crosses[i] == nextCrossId){
                    index03 = i;
                }
            }
        }
        int index01 = 0;
        for (int i = 0; i < crosses.length; i++) {
            if (crosses[i] == crossId){
                index01 = i;
            }
        }
        switch (index03-index01){
            case 1:
                return Direction.L;
            case -3:
                return Direction.L;
            case 2:
                return Direction.D;
            case -2:
                return Direction.D;
            case -1:
                return Direction.R;
            case 3:
                return Direction.R;
            default:
                return Direction.D;
        }
    }

    /**
     * 统计处于某个状态的车辆数量
     * @param carMap    待统计车辆map
     * @param status    要统计的状态
     * @return          状态为status车辆数量
     */
    public static int statusCarCount(Map<Integer, Car> carMap, int status){
        int count = 0;
        for(Integer carId:carMap.keySet()){
            if (carMap.get(carId).getStatus()==status){
                count++;
            }
        }
        return count;
    }

    /**
     * 获取某个路口所有邻接道路等待状态的第一优先级车辆id
     * @param crossId       路口id
     * @param graphInfo     地图信息
     * @return              各邻接道路等待状态第一优先级车辆id
     */
    private static int[] getPriorityCarIdsAtCross(int crossId, GraphInfo graphInfo) {
        int[] neighbourCrossIds = graphInfo.getNeighbourCrossesIdMap().get(crossId);
        int[] priorityCarIds = new int[4];
        for (int i = 0; i < 4; i++) {
            if (neighbourCrossIds[i] != 0){
                if (graphInfo.getEdgeStartEndMap().get(neighbourCrossIds[i]+"_"+crossId)==null){
                    priorityCarIds[i] = 0;
                }else{
                    priorityCarIds[i] = getFirstAwaitCarId(graphInfo.getEdgeStartEndMap().
                            get(neighbourCrossIds[i]+"_"+crossId).getEdgeId(), graphInfo);
                }
            }else {
                priorityCarIds[i] = 0;
            }
        }
        return priorityCarIds;
    }

    /**
     * 返回某条道路等待状态第一优先级车辆id
     * @param edgeId        道路id
     * @param graphInfo     地图信息
     * @return              等待状态第一优先级车辆id
     */
    public static Integer getFirstAwaitCarId(Integer edgeId, GraphInfo graphInfo){
        int carId = 0;
        if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size() != 0){
            Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
            for(Car car:graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge()){
                if (car.getStatus() == CarStatus.Await){
                    carId = car.getCarId();
                    break;
                }
            }
        }
        return carId;
    }

    /**
     * 将路口所有第一优先级车辆id连成字符串，唯一
     * @param priorityCarIdsAtCross     路口第一优先级车辆id数组
     * @return                          唯一字符串
     */
    private static String getAllAwaitCarsFlag(int[] priorityCarIdsAtCross) {
        String awaitIds = "";
        for(int id:priorityCarIdsAtCross){
            awaitIds += id;
        }
        return awaitIds;
    }

    /**
     * 判断本次待驱动车辆是否与本路口其它车辆冲突，false不冲突,true冲突
     * @param carId             驱动的目标车辆id
     * @param priorityCarIds    本路口邻接道路所有第一优先级车辆id
     * @param graphInfo         地图信息
     * @return                  冲突标记符
     */
    private static boolean conflictDiagnose(int carId, int[] priorityCarIds, GraphInfo graphInfo) {
        int barrierToEnd = -1;
        //获取当前道路前方阻挡位置
        for(Car car:graphInfo.getCarIdMap().get(carId).getPosition().edge.getCarsOnThisEdge()){
            if (car.getPosition().lane_No == graphInfo.getCarIdMap().get(carId).getPosition().lane_No &&
                    car.getPosition().disToEnd < graphInfo.getCarIdMap().get(carId).getPosition().disToEnd){
                barrierToEnd = barrierToEnd>car.getPosition().disToEnd?barrierToEnd:car.getPosition().disToEnd;
            }
        }
        if (barrierToEnd == -1){
            return false;
        }
        int carIndex = -1;
        for (int i = 0; i < priorityCarIds.length; i++) {
            if (priorityCarIds[i] == carId){
                carIndex = i;
                break;
            }
        }
        int direction = graphInfo.getCarIdMap().get(carId).getNextCrossDirection();
        switch (direction){
            case Direction.D:
                return false;
            case Direction.L:
                int LconflictIndexD = carIndex-1<0?carIndex+3:carIndex-1;
                //没有车
                if (priorityCarIds[LconflictIndexD] != 0){
                    if (graphInfo.getCarIdMap().get(priorityCarIds[LconflictIndexD]).
                            getNextCrossDirection() == Direction.D){
                        return true;
                    }
                }//有车
                else {
                    return false;
                }
            case Direction.R:
                int RconflictIndexD = carIndex+1>=4?carIndex-3:carIndex+1;
                int RconflictIndexL = carIndex+2>=4?carIndex-2:carIndex+2;
                if (priorityCarIds[RconflictIndexD] != 0){
                    if (graphInfo.getCarIdMap().get(priorityCarIds[RconflictIndexD]).
                            getNextCrossDirection() == Direction.D){
                        return true;
                    }else {
                        if (priorityCarIds[RconflictIndexL] != 0){
                            if (graphInfo.getCarIdMap().get(priorityCarIds[RconflictIndexL]).
                                    getNextCrossDirection() == Direction.L){
                                return true;
                            }else {
                                return false;
                            }
                        }else {
                            return false;
                        }
                    }
                }else {
                    if (priorityCarIds[RconflictIndexL] != 0){
                        if (graphInfo.getCarIdMap().get(priorityCarIds[RconflictIndexL]).
                                getNextCrossDirection() == Direction.L){
                            return true;
                        }else {
                            return false;
                        }
                    }else {
                        return false;
                    }
                }
            default:
                return false;
        }
    }

    /**
     * 调度路口等待状态车辆，如果到达目的地返回true，否则false
     * @param carId         待调度车辆id
     * @param crossId       路口id
     * @param graphInfo     地图信息
     * @return              是否到达终点
     */
    private static boolean runAwaitCarAtCross(Integer carId, Integer crossId, GraphInfo graphInfo){
        Collections.sort(graphInfo.getCarIdMap().get(carId).getPosition().edge.getCarsOnThisEdge(), new CarComparator());
        int barrierToEnd = -1;
        int barrierStatus = CarStatus.No_Status;
        //获取当前道路前方阻挡位置
        for(Car car:graphInfo.getCarIdMap().get(carId).getPosition().edge.getCarsOnThisEdge()){
            if (car.getPosition().lane_No == graphInfo.getCarIdMap().get(carId).getPosition().lane_No &&
                    car.getPosition().disToEnd < graphInfo.getCarIdMap().get(carId).getPosition().disToEnd){
                barrierToEnd = barrierToEnd>car.getPosition().disToEnd?barrierToEnd:car.getPosition().disToEnd;
                barrierStatus = car.getStatus();
            }
        }
        int speed = graphInfo.getCarIdMap().get(carId).getCarVelocity()>graphInfo.getCarIdMap().get(carId).getPosition().edge.getVelocityLim()?
                graphInfo.getCarIdMap().get(carId).getPosition().edge.getVelocityLim():graphInfo.getCarIdMap().get(carId).getCarVelocity();
        //前方阻挡
        if (barrierToEnd != -1){
            if (graphInfo.getCarIdMap().get(carId).getPosition().disToEnd-speed <= barrierToEnd){
                if (barrierStatus == CarStatus.Stop){
                    graphInfo.getCarIdMap().get(carId).getPosition().disToEnd = barrierToEnd + 1;
                    graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                }
            }else {
                graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
                graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
            }
        }//前方没有阻挡，考虑驶入下一条路
        else{
            if (graphInfo.getCarIdMap().get(carId).getPosition().disToEnd < speed){
                if (crossId==graphInfo.getCarIdMap().get(carId).getDestination()){
                    graphInfo.getCarIdMap().get(carId).getPath().add(crossId);
                    graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Arrived);
                    int edgeId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEdgeId();
                    graphInfo.getEdgeIdMap().get(edgeId).removeByCarId(carId);
                    Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
                    return true;
                }else{
                    int nextCrossId = getNextCross(carId, crossId, graphInfo);
					if (graphInfo.getEdgeStartEndMap().get(crossId + "_" + nextCrossId) == null){
						graphInfo.getCarIdMap().get(carId).setNextCrossDirection(Direction.D);
						nextCrossId = getNextCross(carId, crossId, graphInfo);
						//System.out.println(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1) + "_" + crossId + "_" + nextCrossId);
						//System.out.println("直行不通，改直行！");
						if (graphInfo.getEdgeStartEndMap().get(crossId + "_" + nextCrossId) == null){
							//System.out.println(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1) + "_" + crossId + "_" + nextCrossId);
							graphInfo.getCarIdMap().get(carId).setNextCrossDirection(Direction.L);
							nextCrossId = getNextCross(carId, crossId, graphInfo);
							//System.out.println(carId+ "_" + graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1) + "_" + crossId + "_" + nextCrossId);
							//System.out.println("直行不通，改左转！");
							if (graphInfo.getEdgeStartEndMap().get(crossId + "_" + nextCrossId) == null){
								graphInfo.getCarIdMap().get(carId).setNextCrossDirection(Direction.R);
								nextCrossId = getNextCross(carId, crossId, graphInfo);
								//System.out.println(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1) + "_" + crossId + "_" + nextCrossId);
								//System.out.println("左转不通，改右转！");
							}
						}
					}

                    //获得驶过路口后下一个Edge;
                    //System.out.println(crossId + "_" + nextCrossId);
                    int nextEdgeId = graphInfo.getEdgeStartEndMap().get(crossId + "_" + nextCrossId).getEdgeId();
                    Collections.sort(graphInfo.getEdgeIdMap().get(nextEdgeId).getCarsOnThisEdge(), new CarComparator());
                    Position barrierPosition = getBarrierPosition(nextEdgeId, graphInfo);
                    barrierStatus = getBarrierStatus(nextEdgeId, graphInfo);
                    speed = graphInfo.getCarIdMap().get(carId).getCarVelocity()>graphInfo.getEdgeIdMap().get(nextEdgeId).getVelocityLim()?
                            graphInfo.getEdgeIdMap().get(nextEdgeId).getVelocityLim():graphInfo.getCarIdMap().get(carId).getCarVelocity();
                    int nextEdgeRunDist = speed>graphInfo.getCarIdMap().get(carId).getPosition().disToEnd?
                            speed - graphInfo.getCarIdMap().get(carId).getPosition().disToEnd:0;
                    //由于当前edge驶过距离大于下一条edge时速，停在当前路口
                    if (nextEdgeRunDist == 0){
                        graphInfo.getCarIdMap().get(carId).getPosition().disToEnd = 0;
                        graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                    }
                    //下一条路限速较大，不会卡在当前路口
                    else{
                        //全速驶停
                        if (barrierStatus == CarStatus.No_Status || nextEdgeRunDist <
                                graphInfo.getEdgeIdMap().get(nextEdgeId).getLength() - barrierPosition.disToEnd){
                            graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                            barrierPosition.disToEnd = graphInfo.getEdgeIdMap().get(nextEdgeId).getLength() - speed + graphInfo.getCarIdMap().get(carId).getPosition().disToEnd;
                            graphInfo.getCarIdMap().get(carId).getPath().add(crossId);
                            int edgeId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEdgeId();
                            graphInfo.getEdgeIdMap().get(edgeId).removeByCarId(carId);
                            graphInfo.getEdgeIdMap().get(nextEdgeId).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));
                            graphInfo.getCarIdMap().get(carId).setPosition(barrierPosition);
                            Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
                            return true;
                        }
                        else {
                            //不能全速驶停,且障碍是停止车辆
                            if (barrierStatus == CarStatus.Stop){
                                //不在道路末端
                                if (barrierPosition.disToEnd < graphInfo.getEdgeIdMap().get(nextEdgeId).getLength() - 1){
                                    barrierPosition.disToEnd += 1;
                                    graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                                    graphInfo.getCarIdMap().get(carId).getPath().add(crossId);
                                    int edgeId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEdgeId();
                                    graphInfo.getEdgeIdMap().get(edgeId).removeByCarId(carId);
                                    graphInfo.getEdgeIdMap().get(nextEdgeId).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));
                                    graphInfo.getCarIdMap().get(carId).setPosition(barrierPosition);
                                    Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
                                    return true;
                                }//在道路末端
                                else {
                                    graphInfo.getCarIdMap().get(carId).getPosition().disToEnd = 0;
                                    graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
                                }
                            }//不能全速驶停,且障碍是等待车辆
                            else if (barrierStatus == CarStatus.Await){
                                //无需任何操作，该车进入下一波轮询
                            }
                        }
                    }
                    //经过路口后重新规划路线，实时规划
                    int tempId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEdgeId();
                    graphInfo.updateNowCarGraphInfo(graphInfo.getCarIdMap().get(carId));
                    ArrayList<Integer> pastPath = new ArrayList<>();
                    pastPath.addAll(graphInfo.getCarIdMap().get(carId).getPath());
                    int frontCrossId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEndVertex().getVertexId();
                    ArrayList<Integer> tempPath = Dijkstra.getShortPath(frontCrossId,
                            graphInfo.getCarIdMap().get(carId).getDestination(), graphInfo.getVertexIdMap(), graphInfo.getVertexEdgeListMap(),
                            graphInfo.getEdgeIdMap().get(tempId).getStartVertex().getVertexId());
                    Collections.reverse(tempPath);
                    pastPath.addAll(tempPath);
                    graphInfo.getCrossPathCarIdMap().put(carId, pastPath);
                    graphInfo.getCarIdMap().get(carId).setNextCrossDirection(
                            Dispatch.setDirectionByCross(graphInfo.getEdgeIdMap().get(tempId).getStartVertex().getVertexId(),
                                    carId,graphInfo));
                }
            }else {
                graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
                graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
            }
        }
        return false;
    }

    /**
     * 利用车前路口获取下一个路口id
     * @param carId         车辆id
     * @param crossId       车前路口id
     * @param graphInfo     地图信息
     * @return              下一个路口id
     */
    private static int getNextCross(Integer carId, Integer crossId, GraphInfo graphInfo){
        int[] crossIdArray = graphInfo.getNeighbourCrossesIdMap().get(crossId);
        int index = 0;
        for (int i = 0; i < crossIdArray.length; i++) {
            if (crossIdArray[i] == graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size() - 1)){
                index = i;
                break;
            }
        }
        int nextIndex = index + graphInfo.getCarIdMap().get(carId).getNextCrossDirection();
        if (nextIndex >= 4){
            nextIndex -= 4;
        }else if(nextIndex < 0){
            nextIndex += 4;
        }
        return crossIdArray[nextIndex];
    }

    /**
     * 获取下一条道路障碍状态
     * @param edgeId        下一条道路id
     * @param graphInfo     地图信息
     * @return              障碍状态
     */
    private  static int getBarrierStatus(Integer edgeId, GraphInfo graphInfo){
        int barrierStatus = CarStatus.No_Status;
        int barrierToEnd = -1;
        if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size() != 0){
            Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
            for (int i = 0; i < graphInfo.getEdgeIdMap().get(edgeId).getLane_No(); i++) {
                barrierStatus = CarStatus.No_Status;
                barrierToEnd = -1;
                for(Car car:graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge()){
                    if (car.getPosition().lane_No == i){
                        barrierToEnd = car.getPosition().disToEnd>barrierToEnd?car.getPosition().disToEnd:barrierToEnd;
                        barrierStatus = car.getStatus();
                    }
                }
                if (barrierToEnd < graphInfo.getEdgeIdMap().get(edgeId).getLength() - 1){
                    return barrierStatus;
                }
            }
        }
        return barrierStatus;
    }
}
