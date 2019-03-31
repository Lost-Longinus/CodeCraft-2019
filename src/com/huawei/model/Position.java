package com.huawei.model;

import java.util.Comparator;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Position implements Comparator<Position> {
	public Edge edge;
	public int disToEnd, lane_No;

	public Position() {
	}

	@Override
	public String toString() {
		return "Position{" +
				"edge=" + edge.getEdgeId() +
				", disToEnd=" + disToEnd +
				", lane_No=" + lane_No +
				'}';
	}

	@Override
	public int compare(Position o1, Position o2) {
		if (o1.disToEnd > o2.disToEnd){
		    return 1;
		}else if (o1.disToEnd < o2.disToEnd){
			return -1;
		}else{
			if (o1.lane_No > o2.lane_No){
			    return 1;
			}else if (o1.lane_No < o2.lane_No){
			    return -1;
			}else{
				return 0;
			}
		}
	}
}
