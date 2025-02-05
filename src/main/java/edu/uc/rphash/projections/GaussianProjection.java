package edu.uc.rphash.projections;

import java.util.Random;

public class GaussianProjection implements Projector {
	int RAND_MAX = 2147483647;

	float[] M;
	int n;
	int t;
	Random r;
//	float randn;

	public GaussianProjection(int n, int t, int randomseed) {
		this.n = n;
		this.t = t;
		r = new Random(randomseed);
		GenGauss(n, t);

//		randn = quicksqrt(n);
	}
	
	public GaussianProjection(int n, int t) {
		this.n = n;
		this.t = t;
		r = new Random();
		GenGauss(n, t);

//		randn = quicksqrt(n);
	}

//	float quicksqrt(float b) {
//		float x = 1.1f;
//		char i = 0;
//
//		for (; i < 16; i++) {
//			x = (x + (b / x)) / 2.0f;
//		}
//
//		return x;
//	}

	public GaussianProjection() {
	}

	@Override
	public float[] project(float[] s) {
		return projectGauss(s, M, n, t);
	}

	/*
	 * This is the basic naive random projection method
	 */
	float[] projectGauss(float[] v, float[] M, int n, int t) {
		int i, j;
		float[] r = new float[t];
		float sum;
		for (i = 0; i < t; i++) {
			sum = 0.0f;
			for (j = 0; j < n; j++)
				sum += v[i] * M[i * n + j];
			r[i] = sum * ((float) Math.sqrt((float) 1 / (float) t));// scaled
		}
		return r;
	}

	/*
	 * Generate a 'good enough' gaussian random variate. based on central limit
	 * thm , this is used if better than achipolis projection is needed
	 */
//	float sampleNormal() {
//		int i;
//		float s = 0.0f;
//		for (i = 0; i < 6; i++)
//			s += ((float) r.nextInt()) / RAND_MAX;
//		return s - 3.0f;
//	}

	void GenGauss(int n, int t) {
		M = new float[n * t];
		int i = 0;
		int elem = n * t; // vv see which is faster on you machine
		while (i < elem)
			M[i++] = (float) r.nextGaussian();// sampleNormal();
	}

	@Override
	public void setOrigDim(int n) {
		this.n = n;
	}

	@Override
	public void setProjectedDim(int t) {
		this.t = t;
	}

	@Override
	public void setRandomSeed(long l) {
		this.r =new Random(l);
		
	}

	@Override
	public void init() {
		GenGauss(n, t);
	}

}
