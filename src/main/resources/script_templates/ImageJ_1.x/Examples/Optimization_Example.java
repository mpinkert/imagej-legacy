/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
import ij.IJ;

import ij.gui.Plot;

import ij.plugin.PlugIn;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;

import org.apache.commons.math3.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math3.optimization.direct.SimplexOptimizer;

import org.apache.commons.math3.optimization.fitting.CurveFitter;

import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

/**
 * A demonstration how to use Apache commons math to perform
 * <ul>
 * <li>curve fitting</li>
 * <li>minimization</li>
 * </ul>
 */

public class Optimization_Example implements PlugIn {
	/**
	 * This method gets called by ImageJ / Fiji.
	 *
	 * @param arg can be specified in plugins.config
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String arg) {
		fit();
		optimize();
	}

	/**
	 * Demonstrate how to fit a curve to a set of given data points
	 */
	public void fit() {
		/*
		 * First, we define a parameterized univariate function.
		 * To use the Levenberg-Marquardt strategy, it has to be differentiable.
		 * For demonstration purposes, we define it as a simple logarithmic function:
		 * f(x) = a * log(x) + b
		 */
		ParametricUnivariateFunction function = new ParametricUnivariateFunction() {
			@Override
			public double[] gradient(double x, double[] params) {
				double a = params[0];
				double b = params[1];
				return new double[] {
					Math.log(x),
					1
				};
			}

			@Override
			public double value(double x, double[] params) {
				double a  = params[0];
				double b = params[1];
				return a * Math.log(x) + b;
			}
		};

		// Now we initialize the optimizer and curve fitter.
		LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
		CurveFitter fitter = new CurveFitter(optimizer);

		// and feed it some "observed" data points of the form (x, y)
		double[] x = { 1.1, 20.2, 100.3 };
		double[] y = { 5.9, 4.8, 3.7 };
		for (int i = 0; i < x.length; i++)
			fitter.addObservedPoint(x[i], y[i]);

		// Now we let the optimizer do its thing, feeding some initial guesses for a and b
		double[] result = fitter.fit(function, new double[] { 1, 0 });

		// Let's tell the user about it
		IJ.log("\nCurve fitting example\n"
			+ "---------------------\n\n"
			+ "estimated (a, b) = (" + IJ.d2s(result[0], 5) + ", " + IJ.d2s(result[1], 5) + ")");
		double error = 0;
		for (int i = 0; i < x.length; i++)
			error += Math.pow(y[i] - function.value(x[i], result), 2);
		IJ.log("Mean root of squared error: " + IJ.d2s(Math.sqrt(error / x.length), 5));

		// Now let's even draw a (semi-logarithmic) graph
		// 1) make a list of the log(x) values
		double[] logX = new double[x.length];
		for (int i = 0; i < x.length; i++)
			logX[i] = Math.log(x[i]);
		// 2) make a smooth fit curve
		double xWindow = logX[x.length - 1] - logX[0];
		double yWindow = y[0] - y[x.length - 1];
		double[] logXFit = new double[200];
		double[] yFit = new double[logXFit.length];
		for (int i = 0; i < logXFit.length; i++) {
			logXFit[i] = logX[0] + i * xWindow / (logXFit.length - 1);
			yFit[i] = function.value(Math.exp(logXFit[i]), result);
		}
		// 3) show it
		Plot plot = new Plot("Curve fit", "log(x)", "y", logXFit, yFit);
		plot.addPoints(logX, y, Plot.CROSS);
		// Let's add some margin
		plot.setLimits(logX[0] - xWindow / 10, logX[x.length - 1] + xWindow / 10,
			y[x.length - 1] - yWindow / 10, y[0] + yWindow / 10);
		plot.show();
	}

	/**
	 * Demonstrate how to minimize a parameterized function
	 */
	public void optimize() {
		/*
		 * First, we need to define a parameterized function.
		 * For this example, let's assume that it is hard to get at the
		 * derivative, so we implement a MultivariateRealFunction.
		 * Let's use the Rosenbrock function:
		 * http://en.wikipedia.org/wiki/Rosenbrock_function
		 */
		MultivariateFunction function = new MultivariateFunction() {
			@Override
			public double value(double[] point) {
				double x = point[0];
				double y = point[1];
				return (1 - x) * (1 - x) + 100 * (y - x * x) * (y - x * x);
			}
		};
		SimplexOptimizer optimizer = new SimplexOptimizer(1e-5, 1e-10);
		optimizer.setSimplex(new NelderMeadSimplex(new double[] { 0.2, 0.2 }));
		PointValuePair pair = optimizer.optimize(10000, function, GoalType.MINIMIZE, new double[] { 0, 0 });

		// Now, let's tell the user about it:
		double[] point = pair.getPoint();
		IJ.log("\nMinimization (Rosenbrock function)\n"
			+ "----------------------------------\n\n"
			+ "Minimum found at (" + IJ.d2s(point[0], 5) + ", " + IJ.d2s(point[1], 5) + ")\n"
			+ "with value " + IJ.d2s(pair.getValue(), 5));
	}
}
