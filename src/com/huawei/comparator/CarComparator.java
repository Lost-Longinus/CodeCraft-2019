package com.huawei.comparator;

import com.huawei.model.Car;
import com.huawei.model.Position;

import java.util.Comparator;

/**
 * Created by Pengfei Jin on 2019/3/21.
 */
public class CarComparator implements Comparator<Car> {

	@Override
	public int compare(Car car1, Car car2) {
		Position p1 = car1.getPosition();
		Position p2 = car2.getPosition();
		if (p1.disToEnd > p2.disToEnd){
			return 1;
		}else if (p1.disToEnd < p2.disToEnd){
			return -1;
		}else{
			if (p1.lane_No > p2.lane_No){
				return 1;
			}else if (p1.lane_No < p2.lane_No){
				return -1;
			}else{
				return 0;
			}
		}
	}
}
