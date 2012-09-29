package org.gps.service;

import java.io.StringReader;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.baidu.mapapi.GeoPoint;

import android.util.Xml;


public class XmlUtil {

	public static ArrayList<Marker> getMarkers(String xml) {
		try {
			ArrayList<Marker> markers = null;
			String tkNo = "", time = "", lat = "", lon = "", orgiLat = "", orgiLon = "";
			int isGsm = -1;
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new StringReader(xml));
			int eventType = parser.getEventType();// 产生第一个事件
			while (eventType != XmlPullParser.END_DOCUMENT) {// 只要不是文档结束事件
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					String tagName = parser.getName();// 获取解析器当前指向的元素的名称
					if ("PositionList".equals(tagName)) {
						markers = new ArrayList<Marker>();
					}
					if ("TrackerNo".equals(tagName)) {
						tkNo = parser.nextText();// 获取解析器当前指向元素的下一个文本节点的值
					}
					if ("Lat".equals(tagName)) {
						lat = parser.nextText();
					}
					if ("Lon".equals(tagName)) {
						lon = parser.nextText();
					}
					if ("OrgiLat".equals(tagName)) {
						orgiLat = parser.nextText();
					}
					if ("OrgiLon".equals(tagName)) {
						orgiLon = parser.nextText();
					}
					if ("DateTime".equals(tagName)) {
						time = parser.nextText();
					}
					if ("IsGsm".equals(tagName)) {
						isGsm = new Integer(parser.nextText());
					}
					break;

				case XmlPullParser.END_TAG:
					if ("Position".equals(parser.getName())) {
						GeoPoint p = new History(lat, lon, orgiLat, orgiLon)
								.getCurrentGeoPoint();
						markers.add(new Marker(p, tkNo, time, isGsm));
					}
					break;
				}
				eventType = parser.next();
			}
			return markers;
		} catch (Exception e) {
			return null;
		}
	}
}
