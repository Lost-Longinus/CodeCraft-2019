package com.huawei.service;

import com.huawei.model.Edge;
import com.huawei.model.Vertex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Dijkstra {

	public static void setRoot(Vertex v)
	{
		v.setParent(null);
		v.setAdjuDist(0);
	}

	/**
	 * 按当前地图信息，规划已经上路的车辆最短路径，专门为了预防路口调头
	 * @param startId	    	出发节点
	 * @param destId			到达节点
	 * @param vertexIdMap 		节点信息，注意每次规划前要更新
	 * @param ver_edgeList_map 	key为节点，value为该节点邻接edge
	 * @param backCrossId		车辆身后路口id
	 * @return					规划路径经过路口列表
	 */
	public static ArrayList<Integer> getShortPath(int startId, int destId, Map<Integer, Vertex> vertexIdMap,
									  Map<Vertex, List<Edge>> ver_edgeList_map, int backCrossId){

		for (Edge edge:ver_edgeList_map.get(vertexIdMap.get(startId))) {
			if (edge.getEndVertex() == vertexIdMap.get(backCrossId)){
				edge.setWeight(1000000);//防止调头
			}
		}
		Vertex start = vertexIdMap.get(startId);
		Vertex dest = vertexIdMap.get(destId);
		setRoot(start);//初始化起点
		updateChildren(vertexIdMap.get(startId), ver_edgeList_map);
		ArrayList<Integer> path_No = new ArrayList();
		while((dest.getParent()!=null)&&(!dest.equals(start)))
		{
			path_No.add(dest.getVertexId());
			dest = dest.getParent();
		}
		path_No.add(startId);
		return path_No;
	}

	/**
	 * 按当前地图信息，规划车库中车辆最短路径
	 * @param startId				出发节点
	 * @param destId				到达节点
	 * @param vertexIdMap			节点信息，注意每次规划前要更新
	 * @param ver_edgeList_map		key为节点，value为该节点邻接edge
	 * @return						规划路径经过路口列表
	 */
	public static ArrayList<Integer> getShortPath(int startId, int destId, Map<Integer, Vertex> vertexIdMap,
												  Map<Vertex, List<Edge>> ver_edgeList_map){
		Vertex start = vertexIdMap.get(startId);
		Vertex dest = vertexIdMap.get(destId);
		setRoot(start);//初始化起点
		updateChildren(vertexIdMap.get(startId), ver_edgeList_map);
		ArrayList<Integer> path_No = new ArrayList();
		while((dest.getParent()!=null)&&(!dest.equals(start)))
		{
			path_No.add(dest.getVertexId());
			dest = dest.getParent();
		}
		path_No.add(startId);
		return path_No;
	}
	/**
	 * 从开始节点递归更新邻接表
	 * @param v
	 * @param ver_edgeList_map
	 */
	public static void updateChildren(Vertex v, Map<Vertex, List<Edge>> ver_edgeList_map)
	{
		if (v==null) {
			return;
		}

		//无路可走
		if (ver_edgeList_map.get(v)==null||ver_edgeList_map.get(v).size()==0) {
			return;
		}


		//用来保存每个可达的节点
		List<Vertex> childrenList = new LinkedList<Vertex>();

		for(Edge e:ver_edgeList_map.get(v))
		{
			Vertex childVertex = e.getEndVertex();
			//如果子节点之前未知，则进行初始化，
			//把当前边的开始点默认为子节点的父节点，长度默认为边长加边的起始节点的长度，并修改该点为已经添加过，表示不用初始化
			if(!childVertex.isKnown())
			{
				childVertex.setKnown(true);
				childVertex.setAdjuDist(v.getAdjuDist()+e.getWeight());
				childVertex.setParent(v);
				if (e.getWeight()<100000){
					childrenList.add(childVertex);
				}
			}

			//只有小于的情况下，才更新该点为该子节点父节点,并且更新长度。
			int nowDist = v.getAdjuDist()+e.getWeight();
			if(nowDist>=childVertex.getAdjuDist())
			{
				continue;
			}
			else {
				childVertex.setAdjuDist(nowDist);
				childVertex.setParent(v);
			}
		}
		//更新每一个子节点
		for(Vertex vc:childrenList)
		{
			updateChildren(vc, ver_edgeList_map);
		}
	}
}
