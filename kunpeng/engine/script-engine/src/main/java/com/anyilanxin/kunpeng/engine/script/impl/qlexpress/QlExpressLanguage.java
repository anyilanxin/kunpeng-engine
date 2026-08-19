/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptLanguage;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.*;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.booleanandlogic.*;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.numberandmath.AbsFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.ClosedFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.ClosedOpenFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.OpenClosedFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.OpenFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.operator.BetweenOperator;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.operator.RangeSplitOperator;
import org.springframework.beans.factory.BeanFactory;

/**
 * al express language
 *
 * @author zxuanhong
 * @date 2025-10-10 23:50
 * @since
 */
public final class QlExpressLanguage implements ScriptLanguage {
  private final BeanFactory beanFactory;
  private final Express4Runner express4Runner;

  public QlExpressLanguage(final BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    express4Runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    //    express4Runner.addAlias("not", "not_in");
    loadFunction();
    loadOperator();
  }

  private void loadFunction() {
    addFunction(new DateFunction())
        .addFunction(new DateTimeFunction())
        .addFunction(new TimeFunction())
        .addFunction(new ZonedDateTimeFunction())
        .addFunction(new StringToJsonFunction())
        .addFunction(new StringSubFunction())
        .addFunction(new StringUpperCaseFunction())
        .addFunction(new StringLowerCaseFunction())
        .addFunction(new StringContainsFunction())
        .addFunction(new StringSubAfterFunction())
        .addFunction(new StringSubBeforeFunction())
        .addFunction(new StringStartWithFunction())
        .addFunction(new StringEndWithFunction())
        .addFunction(new StringReplaceFunction())
        .addFunction(new StringSplitFunction())
        .addFunction(new StringMatchesFunction())
        // math
        .addFunction(new AbsFunction())
        .addFunction(new CeilingFunction())
        .addFunction(new FloorFunction())
        .addFunction(new SqrtFunction())
        .addFunction(new ExpFunction())
        .addFunction(new LogFunction())
        .addFunction(new ModuloFunction())
        .addFunction(new OddFunction())
        .addFunction(new EvenFunction())
        // list aggregation
        .addFunction(new SumFunction())
        .addFunction(new CountFunction())
        .addFunction(new MinFunction())
        .addFunction(new MaxFunction())
        .addFunction(new MeanFunction())
        .addFunction(new ProductFunction())
        .addFunction(new MedianFunction())
        .addFunction(new StddevFunction())
        // string
        .addFunction(new StringFunction())
        .addFunction(new StringLengthFunction())
        .addFunction(new StringJoinFunction())
        .addFunction(new TrimFunction())
        .addFunction(new UuidFunction())
        // boolean + misc
        .addFunction(new AllFunction())
        .addFunction(new AnyFunction())
        .addFunction(new IsBlankFunction())
        .addFunction(new IsDefinedFunction())
        .addFunction(new IsEmptyFunction())
        .addFunction(new DefaultFunction())
        // context
        .addFunction(new GetValueFunction())
        .addFunction(new GetEntriesFunction())
        .addFunction(new GetOrElseFunction())
        .addFunction(new ContextPutFunction())
        .addFunction(new ContextMergeFunction())
        .addFunction(new ContextFunction())
        // list operations
        .addFunction(new ListContainsFunction())
        .addFunction(new AppendFunction())
        .addFunction(new ConcatenateFunction())
        .addFunction(new ReverseFunction())
        .addFunction(new SublistFunction())
        .addFunction(new FlattenFunction())
        .addFunction(new DistinctValuesFunction())
        .addFunction(new IndexOfFunction())
        .addFunction(new InsertBeforeFunction())
        .addFunction(new RemoveFunction())
        .addFunction(new UnionFunction())
        .addFunction(new SortFunction())
        // conversion + temporal + rounding
        .addFunction(new NumberFunction())
        .addFunction(new FromJsonFunction())
        .addFunction(new NowFunction())
        .addFunction(new TodayFunction())
        .addFunction(new DayOfWeekFunction())
        .addFunction(new DayOfYearFunction())
        .addFunction(new MonthOfYearFunction())
        .addFunction(new WeekOfYearFunction())
        .addFunction(new LastDayOfMonthFunction())
        .addFunction(new DurationFunction())
        .addFunction(new YearsAndMonthsDurationFunction())
        .addFunction(new DecimalFunction())
        .addFunction(new RandomNumberFunction())
        .addFunction(new RoundUpFunction())
        .addFunction(new RoundDownFunction())
        .addFunction(new RoundHalfUpFunction())
        .addFunction(new RoundHalfDownFunction())
        // misc
        .addFunction(new ModeFunction())
        .addFunction(new PartitionFunction())
        .addFunction(new AssertFunction())
        .addFunction(new ExtractFunction())
        .addFunction(new FromBase64Function())
        .addFunction(new ToBase64Function())
        // interval operators
        .addFunction(new BeforeFunction())
        .addFunction(new AfterFunction())
        .addFunction(new IncludesFunction())
        .addFunction(new DuringFunction())
        .addFunction(new StartsFunction())
        .addFunction(new StartedByFunction())
        .addFunction(new FinishesFunction())
        .addFunction(new FinishedByFunction())
        .addFunction(new MeetsFunction())
        .addFunction(new MetByFunction())
        .addFunction(new OverlapsFunction())
        .addFunction(new OverlapsBeforeFunction())
        .addFunction(new OverlapsAfterFunction())
        .addFunction(new CoincidesFunction())

        // other
        .addFunction(new RangeFunction())

        // 区间
        .addFunction(new ClosedFunction())
        .addFunction(new ClosedOpenFunction())
        .addFunction(new OpenClosedFunction())
        .addFunction(new OpenFunction());
  }

  private void loadOperator() {
    addOperator(new RangeSplitOperator()).addOperator(new BetweenOperator());
  }

  private QlExpressLanguage addFunction(final QLFunction function) {
    express4Runner.addFunction(function.getSignature(), function);
    return this;
  }

  private QlExpressLanguage addOperator(final CustomOperator operator) {
    express4Runner.addOperator(operator.getOperator(), operator);
    return this;
  }

  private QlExpressLanguage addObjFunction(final QLObjectFunction objectFunction) {
    express4Runner.addObjFunction(objectFunction);
    return this;
  }

  @Override
  public ScriptExpression parse(final String expression) {
    return new QlExpressExpression(express4Runner, expression, beanFactory);
  }
}
