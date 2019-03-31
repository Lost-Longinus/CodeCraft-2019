package com.huawei.model;

import java.util.Objects;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Vertex {
	public final static int infinite_dis = 10000000;
	//节点id，只提供getter
	private int vertexId;
	//以下提供getter和setter，在dijkstra中实施调整
	private boolean known;//该节点是否已知
	private int adjuDist;//当前到该节点最短距离
	private Vertex parent;//当前最短路径下，本节点的父节点，反向追溯路径

	public Vertex(int vertexId) {
		this.known = false;
		this.adjuDist = infinite_dis;
		this.parent = null;
		this.vertexId = vertexId;
	}

	public void setKnown(boolean known) {
		this.known = known;
	}

	public void setAdjuDist(int adjuDist) {
		this.adjuDist = adjuDist;
	}

	public void setParent(Vertex parent) {
		this.parent = parent;
	}

	public int getVertexId() {
		return vertexId;
	}

	public boolean isKnown() {
		return known;
	}

	public int getAdjuDist() {
		return adjuDist;
	}

	public Vertex getParent() {
		return parent;
	}

	//用于判断递归计算中vertex是否到达起始节点
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vertex)) {
			throw new ClassCastException("an object to compare with a Vertext must be Vertex");
		}

		if (this.vertexId == 0) {
			throw new NullPointerException("vertexId of Vertex to be compared cannot be null");
		}

		return this.vertexId == ((Vertex) obj).vertexId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vertexId);
	}

	@Override
	public String toString() {
		return "Vertex{" +
				"vertexId=" + vertexId +
				", known=" + known +
				", adjuDist=" + adjuDist +
				", parent=" + parent +
				'}';
	}
}
