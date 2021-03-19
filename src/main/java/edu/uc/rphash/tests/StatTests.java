package edu.uc.rphash.tests;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import edu.uc.rphash.Centroid;
import edu.uc.rphash.Readers.StreamObject;
import edu.uc.rphash.tests.generators.ClusterGenerator;
import edu.uc.rphash.util.AtomicFloat;
import edu.uc.rphash.util.VectorUtil;

public class StatTests {
	Random r;
	AtomicFloat sampRatio;
	public StatTests(float sampRatio) {
		r = new Random();
		this.sampRatio = new AtomicFloat(sampRatio);
	}


	public static float PR(List<float[]> estCentroids, ClusterGenerator gen){
		int count = 0 ;
		List<float[]> data = gen.getData();
		for(int i = 0; i< data.size();i++)
		{
			if(VectorUtil.findNearestDistance(data.get(i), estCentroids)==gen.getLabels().get(i))count++;
		}
		System.out.println(data.size());
		return (float)count/(float)data.size();
	}
	
	public static float PR(List<float[]> estCentroids, List<Integer> labels,List<float[]> data){
		int count = 0 ;

		for(int i = 0; i< data.size();i++)
		{
			if(VectorUtil.findNearestDistance(data.get(i), estCentroids)==labels.get(i))count++;
		}
		return (float)count/(float)data.size();
	}
	
	/** Naive sum of square errors, nothing fancy, baseline from the definition
	 * @param data
	 * @return
	 */
	public static double[] WCSS(List<double[]> data){
		
		double d = data.get(0).length;
		double[] ret = new double[(int)d];
		double[] mean = mean(data);
		
		//compute the squared distance from mean
		for(double[] vec : data)
		{
			for(int i = 0;i<d;i++){
				ret[i] += ((vec[i]-mean[i])*(vec[i]-mean[i]));
			}
		}
		for(int i = 0;i<d;i++){
			ret[i] = ret[i]/(double)(data.size());
		}

		return ret;
	}
	/** Naive vector set mean, nothing fancy, baseline from the definition
	 * @param data
	 * @return
	 */
	public static double[] mean(List<double[]> data){
		int d = data.get(0).length;
		double[] mean = new double[d];
		//sum up all the vectors per dimension
		for(double[] vec : data)
		{
			for(int i = 0;i<d;i++){
				mean[i]+= vec[i];
			}
		}
		
		//divide by set size, by the book
		for(int i = 0;i<d;i++){
			mean[i]/= (double)data.size() ;
		}
		return mean;
	}
	
	
	
	public static double WCSSE(List<float[]> estCentroids, List<float[]> data){
		double count = 0.0 ;
		for(int i = 0; i< data.size();i++)
		{
			count+=VectorUtil.distance(data.get(i),estCentroids.get(VectorUtil.findNearestDistance(data.get(i), estCentroids))) ;
		}
		return count;
	}
	
	
	public static double WCSSE(List<Centroid> estCentroids, List<Centroid> data,boolean skip){
		double count = 0.0 ;
		for(int i = 0; i< data.size();i++)
		{
			count+=VectorUtil.distance(data.get(i).centroid(),estCentroids.get(VectorUtil.findNearestDistance(data.get(i), estCentroids)).centroid()) ;
		}
		return count;
	}
	
	public static double WCSSECentroidsFloat(List<Centroid> estCentroids, List<float[]> data){
		double count = 0.0 ;
		for(int i = 0; i< data.size();i++)
		{
//			count+=VectorUtil.distance(data.get(i),estCentroids.get(VectorUtil.findNearestDistance(new Centroid(data.get(i),0), estCentroids)).centroid()) ;
			count+=VectorUtil.distancesq(data.get(i),estCentroids.get(VectorUtil.findNearestDistance(new Centroid(data.get(i),0), estCentroids)).centroid()) ;
		}
		return count;
	}

	public static double WCSSECentroidsFloat(List<Centroid> estCentroids, StreamObject so){
		double count = 0.0 ;
		while (so.hasNext()) {
			float[] nxt = so.next();
			count+=VectorUtil.distance(nxt,estCentroids.get(VectorUtil.findNearestDistance(new Centroid(nxt,0), estCentroids)).centroid()) ;
		}
		so.reset();
		return count;
	}
	
	
	public static double WCSSEFloatCentroid(List<float[]> estCentroids, List<float[]> data){
		double count = 0.0 ;
		for(int i = 0; i< data.size();i++)
		{
			
			count+=VectorUtil.distance(data.get(i),estCentroids.get(VectorUtil.findNearestDistance(data.get(i), estCentroids))) ;
		}
		return count;
	}
	
	public static double WCSSE(List<float[]> estCentroids, String f,boolean raw) throws IOException{
		double count = 0.0 ;
		StreamObject data = new StreamObject(f,0,raw);
		while(data.hasNext())
		{
			float[] next = data.next();
			count+=VectorUtil.distance(next,estCentroids.get(VectorUtil.findNearestDistance(next, estCentroids))) ;
		}
		return count;
	}

	public static double SSE(List<float[]> estCentroids, ClusterGenerator gen){
		double count = 0.0 ;
		List<float[]> data = gen.getMedoids();
		for(int i = 0; i< data.size();i++)
		{
			count+=VectorUtil.distance(data.get(i),estCentroids.get(VectorUtil.findNearestDistance(data.get(i), estCentroids))) ;
		}
		return count;
	}
	
	private float n = 0;
	private float mean = 0;
	private float M2 = 0;
	public float updateVarianceSample(float[] row){
		
		if(r.nextFloat()>sampRatio.floatValue())return M2/(n-1f);
		
		for(float x : row){
			n++;
			float delta = x - mean;
			mean = mean + delta/n;
			M2 = M2 + delta*(x-mean);
		}	
		if(n<2)return 0;
		return  M2/(n-1f);
	}
	

	private float[] meanv=null;
	private float[] M2v;
	private float[] variance=null;
	public float[] updateVarianceSampleVec(float[] row){
		if(n==0){
			meanv = new float[row.length];
			M2v = new float[row.length];
			variance = new float[row.length];
			for(int i = 0;i<row.length;i++)variance[i] = 1f;
			n++;
		}
		
		if( n>10 && r.nextFloat()>sampRatio.floatValue()){
			return variance;
		}
		
		n++;
		for(int i = 0;i<row.length;i++){
			float x =row[i];
			float delta = x - meanv[i];
			meanv[i] = meanv[i] + delta/n;
			M2v[i] = M2v[i] + delta*(x-meanv[i]);
			variance[i] = M2v[i]/(n-1f);
		}	
		return variance;
	}
	
	public float[] scaleVector(float[] vec)
	{
		if(meanv==null || variance==null)return vec;
		for(int i =0;i<vec.length;i++)vec[i] = (vec[i]-meanv[i])/variance[i];
		return vec;
	}
	
	
	
	public static float varianceSample(List<float[]> data,float sampRatio){
		float n = 0;
		float mean = 0;
		float M2 = 0;
		Random r = new Random();
		
		int len = data.size();
		
		for(int i = 0 ; i<sampRatio*len; i++){
			float[] row = data.get(r.nextInt(len));
			
			for(float x : row){
				n++;
				float delta = x - mean;
				mean = mean + delta/n;
				M2 = M2 + delta*(x-mean);
			}	
		}
		if(n<2)return 0;
		
		return  M2/(n-1f);
	}
	
	
	public static float varianceAll(List<float[]> data){
		float n = 0;
		float mean = 0;
		float M2 = 0;

		for(float[] row : data){
			for(float x : row){
				n++;
				float delta = x - mean;
				mean = mean + delta/n;
				M2 = M2 + delta*(x-mean);
			}	
		}
		if(n<2)return 0;
		
		return  M2/(n-1f);
	}
	
	public static float averageAll(List<float[]> data){
		float n = 0;
		float mean = 0;
		for(float[] row : data){
			for(float x : row){
				n++;
				mean+=x;
			}	
		}return mean/n;
	}
	
	public static float[] varianceCol(List<float[]> data){
		int d =data.get(0).length;
		float[] mean = new float[d];
		float[] M2 = new float[d];
		float[] variance = new float[d];
		float n = 0;

		for(float[] x : data){
			n++;
			for(int i =0;i<d;i++){
				float delta = x[i] - mean[i];
				mean[i] = mean[i] + delta/n;
				M2[i] = M2[i] + delta*(x[i]-mean[i]);
			}
		}
		if(n<2)return new float[d];
		for(int i =0;i<d;i++)
			variance[i] = ((float)M2[i]/(n-1f));
		return variance;
	}
	
	public static float[] meanCols(List<float[]> data){
		int d =data.get(0).length;
		float[] mean = new float[d];
		float n = 0;

		for(float[] x : data){
			n++;
			for(int i =0;i<d;i++){
				float delta = x[i] - mean[i];
				mean[i] = mean[i] + delta/n;
			}
		}
		if(n<2)return new float[d];
		return mean;
	}
	
	public static float[][] meanAndVarianceCols(List<float[]> data){
		int d =data.get(0).length;
		float[] mean = new float[d];
		float[] M2 = new float[d];
		float[] variance = new float[d];
		float n = 0;

		for(float[] x : data){
			n++;
			for(int i =0;i<d;i++){
				float delta = x[i] - mean[i];
				mean[i] = mean[i] + delta/n;
				M2[i] = M2[i] + delta*(x[i]-mean[i]);
			}
		}
		
		float[][] ret = new float[2][];
		
		if(n<2)return ret;
		for(int i =0;i<d;i++)
			variance[i] = ((float)M2[i]/(n-1f));

		ret[0] = mean;
		ret[1] = variance;
		return ret;
	}
	

	public static double variance(double[] row) {
		double n = 0;
		double mean = 0;
		double M2 = 0;
		for(double x : row){
			n++;
			double delta = x - mean;
			mean = mean + delta/n;
			M2 = M2 + delta*(x-mean);
		}
		if(n<2)return 0;
		
		return  M2/(n-1f);
	}
	
	public static float variance(float[] row) {
		double n = 0;
		double mean = 0;
		double M2 = 0;
		for(double x : row){
			n++;
			double delta = x - mean;
			mean = mean + delta/n;
			M2 = M2 + delta*(x-mean);
		}
		if(n<2)return 0;
		
		return  (float) (M2/(n-1f));
	}
	
	public List<float[]> zscorenormalize(List<float[]> X){
		int d = X.get(0).length;
		float[] mean = new float[d];
		float[] M2 = new float[d];
		float[] variance = new float[d];
		float n = 0;

		for(float[] x : X){
			n++;
			for(int i =0;i<d;i++){
				float delta = x[i] - mean[i];
				mean[i] = mean[i] + delta/n;
				M2[i] = M2[i] + delta*(x[i]-mean[i]);
			}
		}
		if(n<2)return X;
		for(int i =0;i<d;i++)
			variance[i] = ((float)M2[i]/(n-1f));
		
		
		for(int j =0;j<X.size();j++)
		{
			float[] tmp = new float[d];
			float[] curvec =X.get(j);
			for(int i =0;i<d;i++){
				tmp[i]=(float) ((curvec[i]-mean[i])/Math.sqrt(variance[i]));
			}
			X.set(j, tmp);
		}
		return X;
	}
	
	static public float[] znormvec(float[] curvec,float[] mean,float[] variance){
		int d = curvec.length;
		float[] tmp = new float[d];
		for(int i =0;i<d;i++){
			tmp[i]=(float) ((curvec[i]-mean[i])/Math.sqrt(variance[i]));
		}
		return tmp;
	}

}
