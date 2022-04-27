package edu.uc.rphash.kneefinder;


import edu.uc.rphash.lsh.LSHkNN;
import edu.uc.rphash.tests.generators.GenerateData;
import edu.uc.rphash.util.Maths;
import edu.uc.rphash.util.VectorUtil;

import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

// to find the knee, modified from " https://github.com/lukehb/137-stopmove/blob/master/src/main/java/onethreeseven/stopmove/algorithm/Kneedle.java   by   Luke Bermingham "

/**
 * Given set of values look for the elbow/knee points.
 * See paper: "Finding a Kneedle in a Haystack: Detecting Knee Points in System Behavior"
 */

public class JythonTest {

    /**
     * Finds the indices of all local minimum or local maximum values.
     * @param data The data to process
     * @param findMinima If true find local minimums, else find local maximums.
     * @return A list of the indices that have local minimum or maximum values.
     */
    private ArrayList<Integer> findCandidateIndices(double[][] data, boolean findMinima){
        ArrayList<Integer> candidates = new ArrayList<>();
        //a coordinate is considered a candidate if both of its adjacent points have y-values
        //that are greater or less (depending on whether we want local minima or local maxima)
        for (int i = 1; i < data.length - 1; i++) {
            double prev = data[i-1][1];
            double cur = data[i][1];
            double next = data[i+1][1];
            boolean isCandidate = (findMinima) ? (prev > cur && next > cur) : (prev < cur && next < cur);
            if(isCandidate){
                candidates.add(i);
            }
        }
        return candidates;
    }


    /**
     * Find the index in the data the represents a most exaggerated elbow point.
     * @param data the data to find an elbow in
     * @return The index of the elbow point.
     */
    private int findElbowIndex(double[] data){

        int bestIdx = 0;
        double bestScore = 0;
        for (int i = 0; i < data.length; i++) {
            double score = Math.abs(data[i]);
            if(score > bestScore){
                bestScore = score;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    /**
     * Prepares the data by smoothing, then normalising into unit range 0-1,
     * and finally, subtracting the y-value from the x-value.
     * @param data The data to prepare.
     * @param smoothingWindow Size of the smoothing window.
     * @return The normalised data.
     */
    private double[][] prepare(double[][] data, int smoothingWindow){

        //smooth the data to make local minimum/maximum easier to find (this is Step 1 in the paper)
       // double[][] smoothedData = Maths.gaussianSmooth2d(data, smoothingWindow);

        //prepare the data into the unit range (step 2 of paper)
       // double[][] normalisedData = Maths.minmaxNormalise(smoothedData);
        double[][] normalisedData = Maths.minmaxNormalise(data);

        //subtract normalised x from normalised y (this is step 3 in the paper)
        for (int i = 0; i < normalisedData.length; i++) {
            normalisedData[i][1] = normalisedData[i][1] - normalisedData[i][0];
        }

        return normalisedData;
    }

    private double computeAverageVarianceX(double[][] data){
        double sumVariance = 0;
        for (int i = 0; i < data.length - 1; i++) {
            sumVariance += data[i + 1][0] - data[i][0];
        }
        return sumVariance / (data.length - 1);
    }

    /**
     * Uses a heuristic to find what may be an elbow in the 1d data.
     * This method is a heuristic so it may return in invalid elbow.
     * If you need guarantees use the other method {@link JythonTest#run(double[][], double, int, boolean)}
     * @param data The
     * @return A possible elbow for this 1d data.
     */
    public double findElbowQuick(double[] data){
        if(data.length <= 1){
            return 0;
        }

        // double[] normalisedData = Maths.minmaxNormalise1d(Maths.gaussianSmooth(data, 3));  // original parameter
        double[] normalisedData = Maths.minmaxNormalise1d(Maths.gaussianSmooth(data, 0));
        

        //do kneedle y'-x' (in this case x' is normalised index value)
        for (int i = 0; i < normalisedData.length; i++) {
            double normalisedIndex = (double)i / data.length;
            normalisedData[i] = normalisedData[i] - normalisedIndex;
        }

        int elbowIdx = findElbowIndex(normalisedData);
        return data[elbowIdx];
    }

    /**
     * This algorithm finds the so-called elbow/knee in the data.
     * See paper: "Finding a Kneedle in a Haystack: Detecting Knee Points in System Behavior"
     * for more details.
     * @param data The 2d data to find an elbow in.
     * @param s How many "flat" points to require before we consider it a knee/elbow.
     * @param smoothingWindow The data is smoothed using Gaussian kernel average smoother, this parameter is the window used for averaging
     *                        (higher values mean more smoothing, try 3 to begin with).
     * @param findElbows Whether to find elbows or knees. true for elbows and false for knees.
     * @return The elbow or knee values.
     */
    public ArrayList<double[]> run(double[][] data, double s, int smoothingWindow, boolean findElbows){

        if(data.length == 0){
            throw new IllegalArgumentException("Cannot find elbow or knee points in empty data.");
        }
        if(data[0].length != 2){
            throw new IllegalArgumentException("Cannot run Kneedle, this method expects all data to be 2d.");
        }

        ArrayList<double[]> localMinMaxPts = new ArrayList<>();
        //do steps 1,2,3 of the paper in the prepare method
        double[][] normalisedData = prepare(data, smoothingWindow);
        
        //find candidate indices (this is step 4 in the paper)
        {
            ArrayList<Integer> candidateIndices = findCandidateIndices(normalisedData, findElbows);
            //ArrayList<Integer> candidateIndices = findCandidateIndices(data, findElbows);
            //go through each candidate index, i, and see if the indices after i are satisfy the threshold requirement
            //(this is step 5 in the paper)
            double step = computeAverageVarianceX(normalisedData);
            step = findElbows ? step * s : step * -s;

            //check each candidate to see if it is a real elbow/knee
            //(this is step 6 in the paper)
            for (int i = 0; i < candidateIndices.size(); i++) {
                Integer candidateIdx = candidateIndices.get(i);
                Integer endIdx = (i + 1 < candidateIndices.size()) ? candidateIndices.get(i+1) : data.length;

                double threshold = normalisedData[candidateIdx][1] + step;

                for (int j = candidateIdx + 1; j < endIdx; j++) {
                    boolean isRealElbowOrKnee = (findElbows) ?
                            normalisedData[j][1] > threshold : normalisedData[j][1] < threshold;
                    if(isRealElbowOrKnee) {
                        localMinMaxPts.add(data[candidateIdx]);
                        break;
                    }
                }
            }
        }
        return localMinMaxPts;
    }
    

// to test the funtion :
    public static void main(String[] args){
    	
    	JythonTest elbowcalculator = new JythonTest();
    	
		double elbowdata[]= new double[90];
		
		for (int i=0 ; i<=89; i++)
		{
			 elbowdata[i] = 89-i;
		}		
		
		
/*		double elbowdata2 [] = 
			  { 7304, 6978, 6666, 6463, 6326, 6048, 6032, 5762, 5742,
			    5398, 5256, 5226, 5001, 4941, 4854, 4734, 4558, 4491,
			    4411, 4333, 4234, 4139, 4056, 4022, 3867, 3808, 3745,
			    3692, 3645, 3618, 3574, 3504, 3452, 3401, 3382, 3340,
			    3301, 3247, 3190, 3179, 3154, 3089, 3045, 2988, 2993,
			    2941, 2875, 2866, 2834, 2785, 2759, 2763, 2720, 2660,
			    2690, 2635, 2632, 2574, 2555, 2545, 2513, 2491, 2496,
			    2466, 2442, 2420, 2381, 2388, 2340, 2335, 2318, 2319,
			    2308, 2262, 2235, 2259, 2221, 2202, 2184, 2170, 2160,
			    2127, 2134, 2101, 2101, 2066, 2074, 2063, 2048, 2031    };
*/		
		double elbowdata2[] = {5000,
				4000,
				3000,
				2000,
				1000,
				900,
				800,
				700,
				600,
				500,
				450,
				400,
				350,
				300,
				250,
				225,
				200,
				175,
				150,
				125,
				100,
				75,
				50,
				25,
				24,
				23,
				22,
				21,
				20,
				19,
				18,
				17,
				16,
				15,
				14,
				13,
				12,
				11,
				10,
				10,
				9,
				9,
				9,
				9,
				9,
				9,
				9,
				9,
				9,
				9,	
    } ;
		
		double elbow_point = elbowcalculator.findElbowQuick(elbowdata2);	
		
    	System.out.print("elbow point value form 1D data : "+ elbow_point);
    	
	      double[][] elbowdata3 = new double[50][2] ;
	      for (int i= 0;i<=49;i++) {
	    	  
	    	  elbowdata3[i][1]= 49-i;}
	    	  
	      for (int i= 0;i<=49;i++)
	      {
	    	  elbowdata3[i][0]= elbowdata2[i];
	      }
	    //  System.out.print("\n" +"elbowdata3 : " + elbowdata3[88][1]);
	      
	   //                  public ArrayList<double[]>  run(double[][] data, double s, int smoothingWindow, boolean findElbows)
	      
	      ArrayList<double[]> elbows = elbowcalculator.run ( elbowdata3,      1 ,          1,                false);
	      
	      System.out.print("\n" + "number of elbow points : " + elbows.size());
	      for (double[] point : elbows) {
              System.out.print("\n" +"Knee point:" + Arrays.toString(point));
              System.out.println("\n" +"No. of clusters complement = " +  point[1] );   
              System.out.println("\n" + "No. of clusters = " +  (elbowdata3.length - point[1]));                  
          }
	      
	      
//      
//	      double[][] testData = new double[][]{
//              new double[]{0,0},
//              new double[]{0.1, 0.55},
//              new double[]{0.2, 0.75},
//              new double[]{0.35, 0.825},
//              new double[]{0.45, 0.875},
//              new double[]{0.55, 0.9},
//              new double[]{0.675, 0.925},
//              new double[]{0.775, 0.95},
//              new double[]{0.875, 0.975},
//              new double[]{1,1}
//      };
//      
//      
//      ArrayList<double[]> kneePoints = new Kneedle().run(testData, 1, 1, false);
//
//      for (double[] kneePoint : kneePoints) {
//    	  System.out.println();
//          System.out.print("Knee point:" + Arrays.toString(kneePoint));
//      }
//	      
//
//	          double[][] testData2 = new double[][]{
//	        	  new double[]	{	 200	,	9	},
//	              new double[]	{	 100	,	8	},
//	              new double[]	{	 75		,	7	},
//	              new double[]	{	 50		,	6	},
//	              new double[]	{	 48		,	5	},
//	              new double[]	{	 45		, 	4	},
//	              new double[]	{	 42		,	3	},
//	              new double[]	{	 40		,	2	},
//	              new double[]	{	 39		,	1	},
//	              new double[]	{	 38		,	0	}     
//              
//	          
//	          };
//	          System.out.print("\n" + testData2[9][0]);
//              
////   public ArrayList<double[]> run(double[][] data, double s, int smoothingWindow, boolean findElbows)             
//              ArrayList<double[]> kneePoints2 = new Kneedle().run(testData2, 0, 1, false);
//
//              for (double[] point : kneePoints2) {
//                  System.out.print("\n" +"Knee point:" + Arrays.toString(point));
//                  System.out.println("\n" +"No. of clusters = " +  point[1] );   
//                  System.out.println("\n" + "No. of clusters = " +  (testData2.length - point[1]));                  
//              } 
                  
              
              
              

	      
	      }
	

}