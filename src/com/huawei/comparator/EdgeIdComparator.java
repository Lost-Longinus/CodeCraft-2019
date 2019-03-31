package com.huawei.comparator;

import java.util.Comparator;

/**
 * Created by Pengfei Jin on 2019/3/21.
 */
public class EdgeIdComparator implements Comparator<Integer> {
	@Override
	public int compare(Integer o1, Integer o2) {
		int edgeId01 = o1>10000?o1-10000:o1;
		int edgeId02 = o2>10000?o2-10000:o2;
		if (edgeId01>edgeId02){
			return 1;
		}else if(edgeId01<edgeId02){
			return -1;
		}else {
			return 0;
		}
	}
}
