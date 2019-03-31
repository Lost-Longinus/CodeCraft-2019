package com.huawei.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Edge {
	//以下只提供getter
	private int edgeId, lane_No;//正向编号取RoadId，反向取RoadId+10000
	private Vertex startVertex, endVertex;//该边为有向边，从startVertex-->endVertex
	//以下提供getter和setter，实时改变有向边权重，搜索当前最优路径
	//若road为单向道，该有向边为road不可行方向时，权重设为100000
	private int length, weight, velocityLim;
	private List<Car> carsOnThisEdge = new ArrayList<>();

	public Edge() {
	}

	public Edge(int edgeId, Vertex startVertex, Vertex endVertex){
		this.edgeId = edgeId;
		this.startVertex = startVertex;
		this.endVertex = endVertex;
	}

	public Edge(int edgeId, Vertex startVertex, Vertex endVertex, int velocityLim, int length, int lane_No) {
		this.edgeId = edgeId;
		this.startVertex = startVertex;
		this.endVertex = endVertex;
		this.velocityLim = velocityLim;
		this.length = length;
		this.lane_No = lane_No;
	}

	public void removeByCarId(Integer carId){
		for (int i = 0; i < carsOnThisEdge.size(); i++) {
			if (carsOnThisEdge.get(i).getCarId() == carId){
			    carsOnThisEdge.remove(i);
			}
		}
	}
	public List<Car> getCarsOnThisEdge() {
		return carsOnThisEdge;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Edge edge = (Edge) o;
		return edgeId == edge.edgeId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(edgeId);
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getEdgeId() {
		return edgeId;
	}

	public int getVelocityLim() {
		return velocityLim;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Vertex getStartVertex() {
		return startVertex;
	}
	public int getEdgeVelocityFloor(){
		int velocity = velocityLim;
		for (Car car : carsOnThisEdge){
			if (car.getCarVelocity()<velocityLim){
				velocity = car.getCarVelocity();
			}
		}
		return velocity;
	}
	public Vertex getEndVertex() {
		return endVertex;
	}

	public int getWeight() {
		return weight;
	}

	public int getLane_No() {
		return lane_No;
	}

	@Override
	public String toString() {
		return "Edge{" +
				"edgeId=" + edgeId +
				", startVertex=" + startVertex.getVertexId() +
				", endVertex=" + endVertex.getVertexId() +
				", carsNum=" + carsOnThisEdge.size() +
				'}';
	}
}
