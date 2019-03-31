package com.huawei.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class Car {
	//以下只提供getter
	private int carId, destination, carVelocity;
	//以下提供getter和setter
	private int startTime, origin;
	//位置为越是先进入出口，则越小
	private Position position = new Position();
	private int nextCrossDirection;
	private int status;
	//path不保存出发时间
	private List<Integer> path = new ArrayList<>();

	public Car(ArrayList<Integer> list) {
		this.carId = list.get(0);
		this.origin = list.get(1);
		this.destination = list.get(2);
		this.carVelocity = list.get(3);
		this.startTime = list.get(4);
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public void setNextCrossDirection(int nextCrossDirection) {
		this.nextCrossDirection = nextCrossDirection;
	}

	public int getCarId() {
		return carId;
	}

	public int getOrigin() {
		return origin;
	}

	public int getDestination() {
		return destination;
	}

	public int getCarVelocity() {
		return carVelocity;
	}

	public int getStartTime() {
		return startTime;
	}

	public Position getPosition() {
		return position;
	}

	public int getNextCrossDirection() {
		return nextCrossDirection;
	}

	public List<Integer> getPath() {
		return path;
	}

	@Override
	public String toString() {
		return "Car{" +
				"carId=" + carId +
				", origin=" + origin +
				", destination=" + destination +
				", carVelocity=" + carVelocity +
				", startTime=" + startTime +
				", status=" + status +
				", position=" + position +
				'}';
	}
}
