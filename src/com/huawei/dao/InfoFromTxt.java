package com.huawei.dao;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Pengfei Jin on 2019/3/17.
 */
public class InfoFromTxt {

	//实例化对象map，返回Map<id, Class>
	public static <T> Map<Integer ,T> getIdMap(String path, Class<T> clazz) throws Exception {
		Map<Integer ,T> map = new TreeMap<>();
		ArrayList<ArrayList<Integer>> lists = dataFromTxt(path);
		for(ArrayList list:lists){
			map.put((Integer) list.get(0), (T) clazz.getConstructors()[0].newInstance(list));
		}
		return map;
	}

	//使用List容纳文本信息，用于构造对象
	public static ArrayList<ArrayList<Integer>> dataFromTxt(String path){
		File file = new File(path);
		ArrayList<ArrayList<Integer>> data = new ArrayList<>();
		try {
			InputStream inputStream = new FileInputStream(file);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
			int rowCount = 0;
			while (( line = bufferedReader.readLine()) != null) {
				if (rowCount >= 1){
					String[] strings = line.substring(1).split("\\D+");
					ArrayList<Integer> list = new ArrayList<>();
					for (int i = 0; i < strings.length; i++) {
						list.add(Integer.parseInt(strings[i]));
					}
					data.add(list);
				}
				rowCount++;
			}
			bufferedReader.close();
			inputStreamReader.close();
			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
}
