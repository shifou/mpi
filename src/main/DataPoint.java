package main;

import java.io.Serializable;
import java.util.List;

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
	
	public static DataPoint getMeanOfCluster(List<DataPoint> cluster){
		int num_points = cluster.size();
		Double x_val = 0d;
		Double y_val = 0d;
		for (DataPoint p : cluster){
			x_val += p.getX();
			y_val += p.getY();
		}
		return new DataPoint(x_val/num_points, y_val/num_points);
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
