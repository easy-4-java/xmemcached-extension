package com.googlecode.xmemcached.extension.geo;

import lombok.extern.slf4j.Slf4j;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GeoTemplate {



	/**
	 * 计算两点之间距离 https://www.cnblogs.com/zhaoyanhaoBlog/p/10121499.html
	 * @param longitude1	：坐标1经度
	 * @param latitude1		：坐标1维度
	 * @param longitude2	：坐标2经度
	 * @param latitude2		：坐标2维度
	 * @return	计算结果（单位：米）
	 */
	public double getDistance(double latitude1, double longitude1, double latitude2, double longitude2) {

		double lat1 = (Math.PI / 180) * latitude1;
		double lat2 = (Math.PI / 180) * latitude2;

		double lon1 = (Math.PI / 180) * longitude1;
		double lon2 = (Math.PI / 180) * longitude2;

//      double Lat1r = (Math.PI/180)*(gp1.getLatitudeE6()/1E6);
//      double Lat2r = (Math.PI/180)*(gp2.getLatitudeE6()/1E6);
//      double Lon1r = (Math.PI/180)*(gp1.getLongitudeE6()/1E6);
//      double Lon2r = (Math.PI/180)*(gp2.getLongitudeE6()/1E6);

		// 地球半径
		double R = 6371;

		// 两点间距离 km，如果想要米的话，结果*1000就可以了
		double d = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1))
				* R;

		return d * 1000;
	}

	/**
	 * 1、计算Sphere模式下两个坐标点的距离（单位：米）
	 * @param longitude1	：坐标1经度
	 * @param latitude1		：坐标1维度
	 * @param longitude2	：坐标2经度
	 * @param latitude2		：坐标2维度
	 * @return	计算结果（单位：米）
	 */
	public double getSphereDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
		return this.getDistance(Ellipsoid.Sphere, latitude1, longitude1, latitude2, longitude2);
	}

	/**
	 * 2、计算WGS84模式下两个坐标点的距离（单位：米）
	 * @param longitude1	：坐标1经度
	 * @param latitude1		：坐标1维度
	 * @param longitude2	：坐标2经度
	 * @param latitude2		：坐标2维度
	 * @return	计算结果（单位：米）
	 */
	public double getWGS84Distance(double latitude1, double longitude1, double latitude2, double longitude2) {
	    return this.getDistance(Ellipsoid.WGS84, latitude1, longitude1, latitude2, longitude2);
	}

	/**
	 * 2、计算指定模式下两个坐标点的距离（单位：米）
	 * @param ellipsoid		：坐标计算模式
	 * @param longitude1	：坐标1经度
	 * @param latitude1		：坐标1维度
	 * @param longitude2	：坐标2经度
	 * @param latitude2		：坐标2维度
	 * @return	计算结果（单位：米）
	 */
	public double getDistance(Ellipsoid ellipsoid, double latitude1, double longitude1, double latitude2, double longitude2) {

		// 1、此处可以传入起始点经纬度
		GlobalCoordinates gpsFrom = new GlobalCoordinates(latitude1, longitude1);

		// 2、此处可以传入目标点经纬度
		GlobalCoordinates gpsTo = new GlobalCoordinates(latitude2, longitude2);

	    // 3、调用计算方法，传入坐标系、经纬度用于计算距离
	    return this.getDistance(gpsFrom, gpsTo, ellipsoid);

	}

	public double getDistance(GlobalCoordinates gpsFrom, GlobalCoordinates gpsTo, Ellipsoid ellipsoid){

        // 1、创建GeodeticCalculator，调用计算方法，传入坐标系、经纬度用于计算距离
        GeodeticCurve geoCurve = new GeodeticCalculator().calculateGeodeticCurve(ellipsoid, gpsFrom, gpsTo);

        // 2、获取计算结果
        return geoCurve.getEllipsoidalDistance();
    }

}