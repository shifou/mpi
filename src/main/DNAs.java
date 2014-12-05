package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

public class DNAs implements Serializable {

	private static final long serialVersionUID = -3237008642462674263L;
	public Vector<DNA> dnas;
	public int length;

	public DNAs(int ll) {
		length = ll;
		dnas = new Vector<DNA>();
	}

	public DNAs(DNAs old) {
		dnas = new Vector<DNA>();
		for (int i = 0; i < old.size(); i++) {
			dnas.add(old.get(i));
		}
	}

	public void add(DNA a) {
		dnas.add(a);
	}

	public static int getDis(DNA a, DNA b) {
		int dis = 0;
		if (a == null) {
			System.out.println("null");
			return 0;
		}
		if (b == null) {
			System.out.println("b null");
			return 0;
		}
		for (int i = 0; i < a.strand.length(); i++) {
			if (a.strand.charAt(i) != b.strand.charAt(i)) {
				dis++;
			}
		}
		return dis;
	}

	public int size() {

		return dnas.size();
	}

	public DNA get(int nextInt) {

		return dnas.get(nextInt);
	}

	public static DNA[] Recentroid(int len, int k, DNAs data) {
		DNA[] res = new DNA[k];
		int[][] ct = new int[k][4];
		HashMap<String, Integer> c2i = new HashMap<String, Integer>();
		c2i.put("A", 0);
		c2i.put("C", 1);
		c2i.put("G", 2);
		c2i.put("T", 3);
		HashMap<Integer, String> i2c = new HashMap<Integer, String>();
		i2c.put(0, "A");
		i2c.put(1, "C");
		i2c.put(2, "G");
		i2c.put(3, "T");
		for (int i = 0; i < k; i++)
			res[i] = new DNA(new StringBuffer(""));
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < k; j++)
				for (int o = 0; o < 4; o++)
					ct[j][o] = 0;
			for (int j = 0; j < data.size(); j++) {
				int id = c2i.get("" + data.get(j).strand.charAt(i));
				ct[data.get(j).centroid][id]++;
			}
			for (int j = 0; j < k; j++) {
				String cc = "A";
				int hold = ct[j][0];
				for (int o = 1; o < 4; o++) {
					if (ct[j][o] > hold) {
						hold = ct[j][o];
						cc = i2c.get(o);
					}
				}
				res[j].strand.append(cc);
			}

		}
		return res;
	}

	public static boolean generate(int len, int num, String fileName) {
		try {
			FileWriter wr = new FileWriter(fileName);
			Random rand = new Random();
			char[] TACG = new char[4];
			TACG[0] = 'T';
			TACG[1] = 'A';
			TACG[2] = 'C';
			TACG[3] = 'G';
			System.out.println("-----"+num);
			for (int i = 0; i < num; i++) {
				//System.out.println("-----"+i);
				String hold = "";
				for (int j = 0; j < len; j++) {
					hold += TACG[rand.nextInt(4)];
				}
				wr.write(hold + "\n");
				wr.flush();
			}
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	public static DNA[] RecentrFromCount(int len, int k, int[] total) {
		DNA[] res = new DNA[k];
		HashMap<String, Integer> c2i = new HashMap<String, Integer>();
		c2i.put("A", 0);
		c2i.put("C", 1);
		c2i.put("G", 2);
		c2i.put("T", 3);
		HashMap<Integer, String> i2c = new HashMap<Integer, String>();
		i2c.put(0, "A");
		i2c.put(1, "C");
		i2c.put(2, "G");
		i2c.put(3, "T");
		for (int i = 0; i < k; i++)
			res[i] = new DNA(new StringBuffer(""));
		for (int i = 0; i < len; i++) 
		{
			for (int j = 0; j < k; j++) 
			{
				String cc = "A";
				int hold = total[i*4*k+j*4];
				for (int m = 1; m < 4; m++) 
				{
					if (total[i*4*k+j*4+m] > hold) {
						hold = total[i*4*k+j*4+m];
						cc = i2c.get(m);
					}
				}
				res[j].strand.append(cc);
			}

		}

		return res;
	}

	public DNA[] getInit(int k) {
		Random random = new Random();
		DNA [] ans = new DNA[k];
		int N = this.size();
		HashSet<Integer> check =new HashSet<Integer>();
		for (int i = 0; i < k; i++) {
			int temp = random.nextInt(N);
			while(check.contains(temp)!=false){
				temp = random.nextInt(N);
			}
			ans[i] = this.get(temp);
			check.add(temp);
		}
		return ans;
	}
}
