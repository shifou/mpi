package main;

import java.io.Serializable;

public class DNA implements Serializable{

	private static final long serialVersionUID = 2296111981212487438L;
	// dna string
	public StringBuffer strand;
	// current cluster label
	public int centroid;
	public DNA(StringBuffer s)
	{
		strand=s;
	}
	public DNA(StringBuffer s, int id)
	{
		strand=s;
		centroid=id;
	}
	public String toString()
	{
		return ""+strand.toString();
	}
}
