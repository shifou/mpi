package main;

public class DNAGenerator {
	public static void main(String[] args) {

		if (args.length != 3) {
			System.out.println("Usage: java DNAGenerator <DNA length> <DNA numbers> <output file>");
			return;
		}

		if (args.length == 3) {
			int len=1;
			int num=1;
			try {
				len = Integer.parseInt(args[0]);
				num = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("length should be integer");
			return;
		}
			String outputFile = args[2];	
			// call static method in DNAs
			if(DNAs.generate(len,num,outputFile))
				System.out.println("done!");
			else
				System.out.println("can not create the output file");
		}
	}
}
