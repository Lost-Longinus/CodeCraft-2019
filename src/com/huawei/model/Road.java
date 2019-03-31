package com.huawei.model;

import java.util.ArrayList;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Road {

	//以下只提供getter
	private int roadId, length, velocityLimit, laneNum, startCross, terminalCross, duplex;

	public Road(ArrayList<Integer> list) {
		this.roadId = list.get(0);
		this.length = list.get(1);
		this.velocityLimit = list.get(2);
		this.laneNum = list.get(3);
		this.startCross = list.get(4);
		this.terminalCross = list.get(5);
		this.duplex = list.get(6);
	}

	public int getRoadId() {
		return roadId;
	}

	public int getLength() {
		return length;
	}

	public int getVelocityLimit() {
		return velocityLimit;
	}

	public int getLaneNum() {
		return laneNum;
	}

	public int getStartCross() {
		return startCross;
	}

	public int getTerminalCross() {
		return terminalCross;
	}

	public int getDuplex() {
		return duplex;
	}

	@Override
	public String toString() {
		return "Road{" +
				"roadId=" + roadId +
				", length=" + length +
				", velocityLimit=" + velocityLimit +
				", laneNum=" + laneNum +
				", startCross=" + startCross +
				", terminalCross=" + terminalCross +
				", duplex=" + duplex +
				'}';
	}
}
