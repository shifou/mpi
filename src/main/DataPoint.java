package main;

import java.io.Serializable;

public class DataPoint implements Serializable {


	private static final long serialVersionUID = 6461086191117704779L;

	private Double x;
	private Double y;
	
	public DataPoint(Double x, Double y){
		this.x = x;
		this.y = y;
	}
	
	public static Double getDistance(DataPoint a, DataPoint b){
		
		Double distance = Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
		return distance;
		
	}
	
	public Double getX(){
		return this.x;
	}
	
	public Double getY(){
		return this.y;
	}
	
	public String toString(){
		return this.x.toString()+","+this.y.toString();
	}
	
	public static DataPoint getMean(DataPoint a, DataPoint b){
		Double x1 = (a.getX() + b.getX())/2;
		Double y1 = (a.getY() + b.getY())/2;
		return new DataPoint(x1, y1);
	}
	
	public static int getClosestPoint(DataPoint p, DataPoint[] centroids){
		Double minDistance = Double.MAX_VALUE;
		int length = centroids.length;
		int best = 0;
		for (int i = 0; i < length; i++){
			Double distance = DataPoint.getDistance(p, centroids[i]);
			if (distance < minDistance){
				minDistance = distance;
				best = i;
			}
		}
		return best;
	}
	
}
