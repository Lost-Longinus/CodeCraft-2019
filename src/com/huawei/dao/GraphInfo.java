package com.huawei.dao;

import com.huawei.model.*;

import java.util.*;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class GraphInfo {

	private Map<Integer, Car> carIdMap = new TreeMap<>();//验证一下map是否按id升序排列
	private Map<Integer, Cross> crossIdMap = new TreeMap<>();
	private Map<Integer, Road> roadIdMap = new TreeMap<>();
	private Map<Integer, Vertex> vertexIdMap = new TreeMap<>();
	private Map<Integer, Edge> edgeIdMap = new TreeMap<>();
	private Map<String, Edge> edgeStartEndMap = new HashMap<>();
	private Map<Integer, int[]> neighbourCrossesIdMap = new TreeMap<>();
	private Map<Integer, ArrayList<Integer>> crossPathCarIdMap = new TreeMap<>();//存储规划临时路径
	private List<Integer> carIdList = new ArrayList<>();
	private  Map<String , Road> RoadStartEndMap = new HashMap<>();
	private List<Integer> crossIdList = new ArrayList<>();
	private List<Integer> roadIdList = new ArrayList<>();
	private  List<Vertex> vertexList = new ArrayList<>();
	private  List<Edge> edgeList = new ArrayList<>();
	private Map<Vertex, List<Edge>> vertexEdgeListMap = new HashMap<>();

	public GraphInfo(Map<Integer, Car> carIdMap,
						   Map<Integer, Road> roadIdMap,
						   Map<Integer, Cross> crossIdMap) {
		List<Cross> crosses = initCrosses(crossIdMap);
		List<Road> roads = initRoads(roadIdMap);
		this.carIdMap = carIdMap;
		this.crossIdMap = crossIdMap;
		this.roadIdMap = roadIdMap;
		initCarIdList(carIdMap);
		initCrossIdList(crossIdMap);
		initRoadIdList(roadIdMap);
		initRoadStartEndMap(roads);
		//以下初始化有严格顺序！！
		initVertexList(crosses);
		initVertexIdMap(vertexList);

		initEdgeList(roads);
		initEdgeIdMap(edgeList);
		initEdgeStartEndMap(edgeList);

		initRoadStartEndMap(roads);
		initNeighbourCrossesIdMap(crosses, roadIdMap);

		initVertexEdgeListMap();
	}

	private void initRoadIdList(Map<Integer, Road> roadIdMap) {
		Iterator<Integer> iterator = roadIdMap.keySet().iterator();
		while (iterator.hasNext()){
			roadIdList.add(iterator.next());
		}
	}

	private void initCrossIdList(Map<Integer, Cross> crossIdMap) {
		Iterator<Integer> iterator = crossIdMap.keySet().iterator();
		while (iterator.hasNext()){
			crossIdList.add(iterator.next());
		}
	}

	private void initCarIdList(Map<Integer, Car> carIdMap) {
		Iterator<Integer> iterator = carIdMap.keySet().iterator();
		while (iterator.hasNext()){
			carIdList.add(iterator.next());
		}
	}

	public void updateNowCarGraphInfo(Car car) {
		resetVertexList();
		setEdgeWeightByCar(car);//设置目前行驶车辆的边权
	}
	private void resetVertexList() {
		for(Vertex vertex:vertexList){
			vertex.setAdjuDist(Vertex.infinite_dis);
			vertex.setKnown(false);
			vertex.setParent(null);
		}
	}

	public Map<String, Edge> getEdgeStartEndMap() {
		return edgeStartEndMap;
	}

	public List<Edge> getEdgeList() {
		return edgeList;
	}

	public Map<Integer, Car> getCarIdMap() {
		return carIdMap;
	}

	public Map<Integer, Vertex> getVertexIdMap() {
		return vertexIdMap;
	}

	public Map<Integer, Cross> getCrossIdMap() {
		return crossIdMap;
	}

	public Map<Integer, int[]> getNeighbourCrossesIdMap() {
		return neighbourCrossesIdMap;
	}

	public List<Integer> getCarIdList() {
		return carIdList;
	}

	public List<Integer> getCrossIdList() {
		return crossIdList;
	}

	public Map<String, Road> getRoadStartEndMap() {
		return RoadStartEndMap;
	}

	public Map<Vertex, List<Edge>> getVertexEdgeListMap() {
		return vertexEdgeListMap;
	}

	public Map<Integer, Edge> getEdgeIdMap() {
		return edgeIdMap;
	}

	public Map<Integer, ArrayList<Integer>> getCrossPathCarIdMap() {
		return crossPathCarIdMap;
	}

	public void setCrossPathCarIdMap(Map<Integer, ArrayList<Integer>> crossPathCarIdMap) {
		this.crossPathCarIdMap = crossPathCarIdMap;
	}

	private List<Road> initRoads(Map<Integer, Road> roadIdMap) {
		List<Road> roads = new ArrayList<>();
		for(Integer roadId:roadIdMap.keySet()){
			roads.add(roadIdMap.get(roadId));
		}
		return roads;
	}


	private List<Cross> initCrosses(Map<Integer, Cross> crossIdMap) {
		List<Cross> crosses = new ArrayList<>();
		for(Integer crossId:crossIdMap.keySet()){
			crosses.add(crossIdMap.get(crossId));
		}
		return crosses;
	}

	private void initVertexEdgeListMap(){
		for(Vertex vertex:vertexList){
			List<Edge> list = new ArrayList<>();
			for(Integer endid:neighbourCrossesIdMap.get(vertex.getVertexId())){
				if (endid != 0){
					String startEnd = vertex.getVertexId() + "_";
					startEnd += endid;
					if (edgeStartEndMap.get(startEnd)!=null){
						list.add(edgeStartEndMap.get(startEnd));
					}
				}else{
					continue;
				}
			}
			vertexEdgeListMap.put(vertex, list);
		}
	}
	public  void initVertexList(List<Cross> crosses){
		for(Cross cross:crosses){
			vertexList.add(new Vertex(cross.getCrossId()));
		}
	}
	private void initEdgeList(List<Road> roads){
		for(Road road:roads){
			//所有道路均视为双车道，如果是单向，则设置反向路程为一个极大值(1000000)
			Edge edge1 = new Edge(road.getRoadId(),
					vertexIdMap.get(road.getStartCross()),
					vertexIdMap.get(road.getTerminalCross()),
					road.getVelocityLimit(),
					road.getLength(), road.getLaneNum());

			//正向编号取RoadId，反向取RoadId+10000
			// 注意edge的起始点！！！
			edgeList.add(edge1);
			Edge edge2 = new Edge(road.getRoadId() + 10000,
					vertexIdMap.get(road.getTerminalCross()),
					vertexIdMap.get(road.getStartCross()),
					road.getVelocityLimit(),
					road.getLength(), road.getLaneNum());
			if (road.getDuplex() == 1){
				edgeList.add(edge2);
			}
			//正向车道用道路id标记，反向车道号用道路id+10000标记
		}
	}

	private void initVertexIdMap(List<Vertex> vertices){
		for(Vertex vertex:vertices){
			vertexIdMap.put(vertex.getVertexId(), vertex);
		}
	}

	private void initEdgeIdMap(List<Edge> edgeList){
		for(Edge edge:edgeList){
			edgeIdMap.put(edge.getEdgeId(), edge);
		}
	}

	private void initEdgeStartEndMap(List<Edge> edges){
		for(Edge edge:edges){
			String startEnd = edge.getStartVertex().getVertexId() + "_" + edge.getEndVertex().getVertexId();
			edgeStartEndMap.put(startEnd, edge);
		}
	}

	private void initRoadStartEndMap(List<Road> roads) {
		for(Road road:roads){
			String  startEnd = road.getStartCross() + "_" + road.getTerminalCross();
			RoadStartEndMap.put(startEnd, road);
		}
	}

	private void initNeighbourCrossesIdMap(List<Cross> crosses, Map<Integer, Road> roadIdMap){
		for(Cross cross:crosses){
			int[] neighbourCrossArray = new int[4];
			if (cross.getNorthRoadId() != 1){
				Road road = roadIdMap.get(cross.getNorthRoadId());
				if (road.getStartCross() != cross.getCrossId()){
					neighbourCrossArray[0] = road.getStartCross();
				}else{
					neighbourCrossArray[0] = road.getTerminalCross();
				}
			}else{
				neighbourCrossArray[0] = 0;
			}
			if (cross.getEastRoadId() != 1){
				Road road = roadIdMap.get(cross.getEastRoadId());
				if (road.getStartCross() != cross.getCrossId()){
					neighbourCrossArray[1] = road.getStartCross();
				}else{
					neighbourCrossArray[1] = road.getTerminalCross();
				}
			}else{
				neighbourCrossArray[1] = 0;
			}
			if (cross.getSouthRoadId() != 1){
				Road road = roadIdMap.get(cross.getSouthRoadId());
				if (road.getStartCross() != cross.getCrossId()){
					neighbourCrossArray[2] = road.getStartCross();
				}else{
					neighbourCrossArray[2] = road.getTerminalCross();
				}
			}else{
				neighbourCrossArray[2] = 0;
			}
			if (cross.getWestRoadId() != 1){
				Road road = roadIdMap.get(cross.getWestRoadId());
				if (road.getStartCross() != cross.getCrossId()){
					neighbourCrossArray[3] = road.getStartCross();
				}else{
					neighbourCrossArray[3] = road.getTerminalCross();
				}
			}else{
				neighbourCrossArray[3] = 0;
			}
			neighbourCrossesIdMap.put(cross.getCrossId(), neighbourCrossArray);
		}
	}

	//待优化，调度策略核心算法
	private void setEdgeWeightByCar(Car car){
		for(Edge edge:edgeList){
			int totalPos = edge.getLength()*edge.getLane_No();
			if (edge.getCarsOnThisEdge().size()/totalPos<0.9){
				edge.setWeight(1500);
			}else{
				edge.setWeight(10000);
			}
			if (edge.getEdgeVelocityFloor() != 0){
				if (car.getCarVelocity()>edge.getEdgeVelocityFloor()){
					edge.setWeight(edge.getWeight()+1000*car.getCarVelocity()/edge.getEdgeVelocityFloor());
				}
			}
		}
	}
}
