package com.huawei.service;

import com.huawei.comparator.CarComparator;
import com.huawei.comparator.EdgeIdComparator;
import com.huawei.dao.GraphInfoForAns;
import com.huawei.model.*;

import java.util.*;

/**
 * Created by Pengfei Jin on 2019/3/19.
 */

public class DispatchForAnswer {
	public static void initDispatch(GraphInfoForAns graphInfo){
		for(Integer carId:graphInfo.getCarIdMap().keySet()){
			graphInfo.getCarIdMap().get(carId).setStartTime(graphInfo.getTimeCrossPathCarIdMap().get(carId).get(0));
		}
		for (int i = 0; i < graphInfo.getCarIdList().size(); i++) {
			int carId = graphInfo.getCarIdList().get(i);
			if (graphInfo.getCarIdMap().get(carId).getStartTime() == 1){
				String startEnd = graphInfo.getTimeCrossPathCarIdMap().get(carId).get(1)+"_"+graphInfo.getTimeCrossPathCarIdMap().get(carId).get(2);
				Position barrierPosition = getBarrierPosition(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId(), graphInfo);
				int speed = graphInfo.getEdgeIdMap().get(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId()).getVelocityLim()>graphInfo.getCarIdMap().get(carId).getCarVelocity()?
						graphInfo.getCarIdMap().get(carId).getCarVelocity():graphInfo.getEdgeIdMap().get(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId()).getVelocityLim();
				//路被堵住
				if (barrierPosition.disToEnd == (graphInfo.getEdgeStartEndMap().get(startEnd).getLength()-1) &&
						barrierPosition.lane_No == graphInfo.getEdgeStartEndMap().get(startEnd).getLane_No() - 1){
					graphInfo.getCarIdMap().get(carId).setStartTime(graphInfo.getCarIdMap().get(carId).getStartTime()+1);
				} else {
					barrierPosition.disToEnd = barrierPosition.disToEnd >= graphInfo.getEdgeStartEndMap().get(startEnd).getLength() - speed?
							barrierPosition.disToEnd + 1:graphInfo.getEdgeStartEndMap().get(startEnd).getLength() - speed;
					graphInfo.getCarIdMap().get(carId).setPosition(barrierPosition);
					graphInfo.getEdgeStartEndMap().get(startEnd).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));
					graphInfo.getCarIdMap().get(carId).getPath().add(graphInfo.getTimeCrossPathCarIdMap().get(carId).get(1));
					graphInfo.getCarIdMap().get(carId).setNextCrossDirection(
							setDirectionByCross(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1), carId, graphInfo));
					graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
				}
			}
		}
	}

	//！！！注意，调用该方法前，使用graphInfo的副本，如果返回值为true，才能将该副本赋值给graphInfo，否则重新规划路线
	//dispatchCarOnRoad方法应用于每一个时间片
	public static boolean dispatchCarOnRoad(GraphInfoForAns graphInfo){
		//用于记录每次调度后等待状态车辆数量，如果两次调度未能降低该值，则陷入死循环，返回false
		int totalAwaitCount = 0;
		//第二步：：调度所有车，处理掉够驶停的车辆调度所有车，处理掉够驶停的车辆
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
					for(Car car:graphInfo.getCarIdMap().get(carId).getPosition().edge.getCarsOnThisEdge()){
						if (car.getPosition().lane_No == graphInfo.getCarIdMap().get(carId).getPosition().lane_No &&
								car.getPosition().disToEnd < graphInfo.getCarIdMap().get(carId).getPosition().disToEnd){
							barrierToEnd = barrierToEnd>car.getPosition().disToEnd?barrierToEnd:car.getPosition().disToEnd;
							barrierStatus = car.getStatus();
						}
					}
					if (barrierToEnd != -1){
						if (graphInfo.getCarIdMap().get(carId).getPosition().disToEnd-speed <= barrierToEnd){
							if (barrierStatus == CarStatus.Stop){
								graphInfo.getCarIdMap().get(carId).getPosition().disToEnd = barrierToEnd + 1;
								graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
							}else{
								graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Await);
							}
						}else {
							graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
							graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
						}
					}else{
						if (graphInfo.getCarIdMap().get(carId).getPosition().disToEnd-speed <= barrierToEnd){
							if (edge.getEndVertex().getVertexId() == edge.getCarsOnThisEdge().get(i).getDestination()){
								//该车到达目的地
								edge.getCarsOnThisEdge().get(i).setStatus(CarStatus.Arrived);
								edge.getCarsOnThisEdge().get(i).getPosition().disToEnd = -1;
								edge.getCarsOnThisEdge().get(i).getPath().add(edge.getEndVertex().getVertexId());
								edge.removeByCarId(carId);
								i--;//!!!!!!!!注意列表索引向前进一位
								Collections.sort(edge.getCarsOnThisEdge(), new CarComparator());
							}else{
								graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Await);
							}
						}else{
							graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
							graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
						}
					}
				}
			}
		}
		//第三步：：按路口升序，调度路口堵塞车辆
		//调度前，记录等待车辆数目
		totalAwaitCount = statusCarCount(graphInfo.getCarIdMap(), CarStatus.Await);
		while (totalAwaitCount != 0){
			crossLoop:for(Integer crossId:graphInfo.getCrossIdList()){
				int[] crossIds = graphInfo.getNeighbourCrossesIdMap().
						get(crossId);
				List<Integer> edgeIds = new ArrayList<>();
				for (int i = 0; i < crossIds.length; i++) {
					if (crossIds[i] == 0){
						edgeIds.add(0);
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
				String crossFlag = getAllAwaitCarsFlag(getPriorityCarIdsAtCross(crossId, graphInfo));;//用于记录该路口是否发生死锁
				//路口按id升序轮询
				while (!crossFlag.equals("0000")){
					//System.out.println("crossFlag="+crossFlag);
					edgeLoop:for(Integer edgeId:edgeIds) {
						if (edgeId != 0) {
							if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size() != 0) {
								for (int i = 0; i < graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().size(); i++) {
									int priorityIndex = getFirstAwaitCarId(edgeId, graphInfo);
									if (graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().get(i).getCarId() == priorityIndex) {
										//判断第一优先级是否与其它道路发生冲突
										if (conflictDiagnose(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().get(i).getCarId(),
												//获取路口所有第一优先级车辆id，用于判断冲突,false不冲突
												getPriorityCarIdsAtCross(crossId, graphInfo), graphInfo) == false) {
											boolean flag = runAwaitCarAtCross(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge().get(i).getCarId(), crossId, graphInfo);
											if (flag){
											    i--;
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
					String  tempAllAwaitCarsFlag = getAllAwaitCarsFlag(getPriorityCarIdsAtCross(crossId, graphInfo));
					//System.out.println("tempAllAwaitCarsFlag"+tempAllAwaitCarsFlag);
					if (tempAllAwaitCarsFlag.equals(crossFlag)){
						continue crossLoop;
					}else {
						crossFlag = tempAllAwaitCarsFlag;
					}
				}
			}
			//本次按路口升序调度结束，比较被调度车辆是否发生变化，没有则说明发生死锁，返回false
			int tempCount = statusCarCount(graphInfo.getCarIdMap(), CarStatus.Await);
			if (tempCount - totalAwaitCount == 0){
				return false;
			}else{
				totalAwaitCount = tempCount;
			}
		}
		return true;
	}

	//用于调度车库中的车辆
	public static void fromGarageToRoad(GraphInfoForAns graphInfo, int startTime){
		for(Integer carId:graphInfo.getCarIdList()){
			if (graphInfo.getCarIdMap().get(carId).getStartTime() == startTime){
				String startEnd = graphInfo.getTimeCrossPathCarIdMap().get(carId).get(1)+"_"+graphInfo.getTimeCrossPathCarIdMap().get(carId).get(2);
				Position barrierPosition = getBarrierPosition(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId(), graphInfo);
				//路是空的
				int speed = graphInfo.getEdgeIdMap().get(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId()).getVelocityLim()>graphInfo.getCarIdMap().get(carId).getCarVelocity()?
						graphInfo.getCarIdMap().get(carId).getCarVelocity():graphInfo.getEdgeIdMap().get(graphInfo.getEdgeStartEndMap().get(startEnd).getEdgeId()).getVelocityLim();
				if (barrierPosition.disToEnd == (graphInfo.getEdgeStartEndMap().get(startEnd).getLength()-1) &&
						barrierPosition.lane_No == graphInfo.getEdgeStartEndMap().get(startEnd).getLane_No() - 1){
					graphInfo.getCarIdMap().get(carId).setStartTime(graphInfo.getCarIdMap().get(carId).getStartTime()+1);
				} else {
					barrierPosition.disToEnd = barrierPosition.disToEnd >= graphInfo.getEdgeStartEndMap().get(startEnd).getLength() - speed?
							barrierPosition.disToEnd + 1:graphInfo.getEdgeStartEndMap().get(startEnd).getLength() - speed;
					graphInfo.getCarIdMap().get(carId).setPosition(barrierPosition);
					graphInfo.getEdgeStartEndMap().get(startEnd).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));
					graphInfo.getCarIdMap().get(carId).getPath().add(graphInfo.getTimeCrossPathCarIdMap().get(carId).get(1));
					graphInfo.getCarIdMap().get(carId).setNextCrossDirection(
							setDirectionByCross(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1), carId, graphInfo));
					graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
				}
			}
		}

	}
	//利用背后路口定方向
	private static int setDirectionByCross(int crossId, int carId, GraphInfoForAns graphInfo){
		int backCrossIndex = graphInfo.getTimeCrossPathCarIdMap().get(carId).lastIndexOf(crossId);
		int frontCrossId = graphInfo.getTimeCrossPathCarIdMap().get(carId).get(backCrossIndex+1);
		int index03 = 100;
		int[] crosses = graphInfo.getNeighbourCrossesIdMap().get(frontCrossId);
		if (graphInfo.getTimeCrossPathCarIdMap().get(carId).size() - backCrossIndex - 2 > 0){
			int nextCrossId = graphInfo.getTimeCrossPathCarIdMap().get(carId).get(backCrossIndex+2);
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
	//返回路口等待车辆id串号
	private static String getAllAwaitCarsFlag(int[] priorityCarIdsAtCross) {
		String awaitIds = "";
		for(int id:priorityCarIdsAtCross){
			awaitIds += id;
		}
		return awaitIds;
	}
	//用于诊断是否发生冲突,false不冲突,true冲突
	private static boolean conflictDiagnose(int carId, int[] priorityCarIds, GraphInfoForAns graphInfo) {
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
	//用于获取所有驶入某一路口的第一优先级车辆id,没有车是0，路不通是-1
	private static int[] getPriorityCarIdsAtCross(int crossId, GraphInfoForAns graphInfo) {
		int[] neighbourCrossIds = graphInfo.getNeighbourCrossesIdMap().get(crossId);
		int[] priorityCarIds = new int[4];
		for (int i = 0; i < 4; i++) {
			if (neighbourCrossIds[i] != 0){
			    priorityCarIds[i] = getFirstAwaitCarId(graphInfo.getEdgeStartEndMap().
						get(neighbourCrossIds[i]+"_"+crossId).getEdgeId(), graphInfo);
			}else {
				priorityCarIds[i] = 0;
			}
		}
		return priorityCarIds;
	}
	//用于统计等待状态车数量
	public static int statusCarCount(Map<Integer, Car> carMap, int status){
		int count = 0;
		for(Integer carId:carMap.keySet()){
			if (carMap.get(carId).getStatus()==status){
			    count++;
			}
		}
		return count;
	}
	//驱动一台等待车辆, cross为将要经过的路口,如果到达终点返回true,默认false
	private static boolean runAwaitCarAtCross(Integer carId, Integer crossId, GraphInfoForAns graphInfo){
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
					graphInfo.getEdgeIdMap().get(edgeId).removeByCarId(carId);//能否删除，待验证！！！
					Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
					return true;
				}else{
					int nextCrossId = getNextCross(carId, crossId, graphInfo);
					//获得驶过路口后下一个Edge
					int  nextEdgeId = graphInfo.getEdgeStartEndMap().get(crossId + "_" + nextCrossId).getEdgeId();
					Position barrierPosition = getBarrierPosition(nextEdgeId, graphInfo);
					barrierStatus = getBarrierStatus(nextEdgeId, graphInfo);
					speed = graphInfo.getCarIdMap().get(carId).getCarVelocity()>graphInfo.getEdgeIdMap().get(nextEdgeId).getVelocityLim()?
							graphInfo.getEdgeIdMap().get(nextEdgeId).getVelocityLim():graphInfo.getCarIdMap().get(carId).getCarVelocity();
					int nextEdgeRunDist = speed>graphInfo.getCarIdMap().get(carId).getPosition().disToEnd?
							speed - graphInfo.getCarIdMap().get(carId).getPosition().disToEnd:0;
					//没有障碍或者不会阻挡
					//由于当前edge驶过距离大于下一条edge时速，停在当前路口
					if (nextEdgeRunDist == 0){
						graphInfo.getCarIdMap().get(carId).getPosition().disToEnd = 0;
						graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
					}//下一条路速度足够，不会卡在当前路口
					else{
						//可以全速驶停
						if (barrierStatus == CarStatus.No_Status || nextEdgeRunDist <
								graphInfo.getEdgeIdMap().get(nextEdgeId).getLength() - barrierPosition.disToEnd){
							graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
							barrierPosition.disToEnd = graphInfo.getEdgeIdMap().get(nextEdgeId).getLength() - speed + graphInfo.getCarIdMap().get(carId).getPosition().disToEnd;
							graphInfo.getCarIdMap().get(carId).getPath().add(crossId);
							graphInfo.getCarIdMap().get(carId).setNextCrossDirection(
									setDirectionByCross(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1), carId, graphInfo));
							int edgeId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEdgeId();
							graphInfo.getEdgeIdMap().get(edgeId).removeByCarId(carId);//能否删除，待验证！！！
							graphInfo.getEdgeIdMap().get(nextEdgeId).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));//能否删除，待验证！！！
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
									graphInfo.getCarIdMap().get(carId).setNextCrossDirection(
											setDirectionByCross(graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size()-1), carId, graphInfo));
									int edgeId = graphInfo.getCarIdMap().get(carId).getPosition().edge.getEdgeId();
									graphInfo.getEdgeIdMap().get(edgeId).removeByCarId(carId);//能否删除，待验证！！！
									graphInfo.getEdgeIdMap().get(nextEdgeId).getCarsOnThisEdge().add(graphInfo.getCarIdMap().get(carId));//能否删除，待验证！！！
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
				}
			}else {
				graphInfo.getCarIdMap().get(carId).getPosition().disToEnd -= speed;
				graphInfo.getCarIdMap().get(carId).setStatus(CarStatus.Stop);
			}
		}
		return false;
	}
	//获取某台车下一个行驶路口
	private static int getNextCross(Integer carId, Integer crossId, GraphInfoForAns graphInfo){
		int[] crossIdArray = graphInfo.getNeighbourCrossesIdMap().get(crossId);
		int index = 0;
		for (int i = 0; i < crossIdArray.length; i++) {
			if (crossIdArray[i] == graphInfo.getCarIdMap().get(carId).getPath().get(graphInfo.getCarIdMap().get(carId).getPath().size() - 1)){
			    index = i;
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
	//获取障碍位置,没有障碍则disToEnd=-1
	private static Position getBarrierPosition(Integer edgeId, GraphInfoForAns graphInfo){
		int barrierToEnd = -1;
		Position barrierPosition = new Position();
		Collections.sort(graphInfo.getEdgeIdMap().get(edgeId).getCarsOnThisEdge(), new CarComparator());
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
	//获取障碍状态
	private  static int getBarrierStatus(Integer edgeId, GraphInfoForAns graphInfo){
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
	//获取某条edge第一辆处于等待状态车辆id，没有车返回0
	private static Integer getFirstAwaitCarId(Integer edgeId, GraphInfoForAns graphInfo){
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
}
