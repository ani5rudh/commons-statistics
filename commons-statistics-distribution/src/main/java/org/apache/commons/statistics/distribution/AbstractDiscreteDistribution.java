/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InverseTransformDiscreteSampler;

/**
 * Base class for integer-valued discrete distributions.  Default
 * implementations are provided for some of the methods that do not vary
 * from distribution to distribution.
 */
abstract class AbstractDiscreteDistribution
    implements DiscreteDistribution {

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns:
     * <ul>
     * <li>{@link #getSupportLowerBound()} for {@code p = 0},</li>
     * <li>{@link #getSupportUpperBound()} for {@code p = 1}, or</li>
     * <li>the result of a binary search between the lower and upper bound using
     *     {@link #cumulativeProbability(int)}. The bounds may be bracketed for
     *     efficiency.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code p < 0} or {@code p > 1}
     */
    @Override
    public int inverseCumulativeProbability(final double p) {
        ArgumentUtils.checkProbability(p);

        int lower = getSupportLowerBound();
        if (p == 0) {
            return lower;
        }
        if (lower == Integer.MIN_VALUE) {
            if (checkedCumulativeProbability(lower) >= p) {
                return lower;
            }
        } else {
            lower -= 1; // this ensures cumulativeProbability(lower) < p, which
                        // is important for the solving step
        }

        int upper = getSupportUpperBound();
        if (p == 1) {
            return upper;
        }

        // use the one-sided Chebyshev inequality to narrow the bracket
        // cf. AbstractRealDistribution.inverseCumulativeProbability(double)
        final double mu = getMean();
        final double sigma = Math.sqrt(getVariance());
        final boolean chebyshevApplies = Double.isFinite(mu) &&
                                         Double.isFinite(sigma) &&
                                         sigma != 0.0;

        if (chebyshevApplies) {
            double k = Math.sqrt((1.0 - p) / p);
            double tmp = mu - k * sigma;
            if (tmp > lower) {
                lower = ((int) Math.ceil(tmp)) - 1;
            }
            k = 1.0 / k;
            tmp = mu + k * sigma;
            if (tmp < upper) {
                upper = ((int) Math.ceil(tmp)) - 1;
            }
        }

        return solveInverseCumulativeProbability(p, lower, upper);
    }

    /**
     * This is a utility function used by {@link
     * #inverseCumulativeProbability(double)}. It assumes {@code 0 < p < 1} and
     * that the inverse cumulative probability lies in the bracket {@code
     * (lower, upper]}. The implementation does simple bisection to find the
     * smallest {@code p}-quantile {@code inf{x in Z | P(X <= x) >= p}}.
     *
     * @param p Cumulative probability.
     * @param lowerBound Value satisfying {@code cumulativeProbability(lower) < p}.
     * @param upperBound Value satisfying {@code p <= cumulativeProbability(upper)}.
     * @return the smallest {@code p}-quantile of this distribution.
     */
    private int solveInverseCumulativeProbability(final double p,
                                                  int lowerBound,
                                                  int upperBound) {
        // Use long to prevent overflow during computation of the middle
        long lower = lowerBound;
        long upper = upperBound;
        while (lower + 1 < upper) {
            // Cannot replace division by 2 with a right shift because (lower + upper)
            // can be negative. This can be optimized when we know that both
            // lower and upper arguments of this method are positive, for
            // example, for PoissonDistribution.
            final long middle = (lower + upper) / 2;
            final double pm = checkedCumulativeProbability((int) middle);
            if (pm >= p) {
                upper = middle;
            } else {
                lower = middle;
            }
        }
        return (int) upper;
    }

    /**
     * Computes the cumulative probability function and checks for {@code NaN}
     * values returned. Rethrows any exception encountered evaluating the cumulative
     * probability function. Throws {@code IllegalStateException} if the cumulative
     * probability function returns {@code NaN}.
     *
     * @param argument Input value.
     * @return the cumulative probability.
     * @throws IllegalStateException if the cumulative probability is {@code NaN}.
     */
    private double checkedCumulativeProbability(int argument) {
        final double result = cumulativeProbability(argument);
        if (Double.isNaN(result)) {
            throw new IllegalStateException("Internal error");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public DiscreteDistribution.Sampler createSampler(final UniformRandomProvider rng) {
        // Inversion method distribution sampler.
        return InverseTransformDiscreteSampler.of(rng, this::inverseCumulativeProbability)::sample;
    }
}
