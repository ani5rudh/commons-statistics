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
package org.apache.commons.statistics.descriptive;

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.statistics.distribution.DoubleTolerance;
import org.apache.commons.statistics.distribution.DoubleTolerances;

/**
 * Test for {@link Product} using {@code int} values.
 */
final class IntProductTest extends BaseIntStatisticTest<DoubleAsIntStatistic> {

    @Override
    protected DoubleAsIntStatistic create() {
        return DoubleAsIntStatistic.from(Product.create());
    }

    @Override
    protected DoubleAsIntStatistic create(int... values) {
        return DoubleAsIntStatistic.from(Product.of(values));
    }

    @Override
    protected DoubleStatistic createAsDoubleStatistic(int... values) {
        return Product.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    @Override
    protected DoubleTolerance getToleranceAsDouble() {
        return DoubleTolerances.equals();
    }

    @Override
    protected StatisticResult getEmptyValue() {
        return createStatisticResult(1);
    }

    @Override
    protected StatisticResult getExpectedValue(int[] values) {
        // The product is not high precision
        final double x = Arrays.stream(values).asDoubleStream().reduce(1, (a, b) -> a * b);
        return createStatisticResult(x);
    }

    @Override
    protected DoubleTolerance getTolerance() {
        return DoubleTolerances.ulps(10);
    }

    @Override
    protected Stream<StatisticTestData> streamTestData() {
        final Stream.Builder<StatisticTestData> builder = Stream.builder();
        // Same cases as for the DoubleStatistic Product
        // Python Numpy v1.25.1: numpy.product
        builder.accept(addReference(24.0, DoubleTolerances.ulps(1), 1, 2, 3, 4));
        builder.accept(addReference(3081078000.0, DoubleTolerances.ulps(1), 5, 9, 13, 14, 10, 12, 11, 15, 19));
        return builder.build();
    }
}
