package com.huawei.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Cross {
	//全部只提供getter
	private int crossId, northRoadId, eastRoadId, southRoadId, westRoadId;
	//以下提供getter和setter
	public Cross(ArrayList<Integer> list) {
		this.crossId = list.get(0);
		this.northRoadId = list.get(1);
		this.eastRoadId = list.get(2);
		this.southRoadId = list.get(3);
		this.westRoadId = list.get(4);
	}

	public int getCrossId() {
		return crossId;
	}

	public int getNorthRoadId() {
		return northRoadId;
	}

	public int getEastRoadId() {
		return eastRoadId;
	}

	public int getSouthRoadId() {
		return southRoadId;
	}

	public int getWestRoadId() {
		return westRoadId;
	}

	@Override
	public String toString() {
		return "Cross{" +
				"crossId=" + crossId +
				", northRoadId=" + northRoadId +
				", eastRoadId=" + eastRoadId +
				", southRoadId=" + southRoadId +
				", westRoadId=" + westRoadId +
				'}';
	}
}
