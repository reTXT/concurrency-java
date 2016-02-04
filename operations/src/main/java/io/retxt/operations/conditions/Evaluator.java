package io.retxt.operations.conditions;

import com.google.common.collect.FluentIterable;
import io.retxt.dispatch.DispatchGroup;
import io.retxt.dispatch.DispatchQueues;
import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.conditions.Condition.Result;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.function.Consumer;



/**
 * Evaluates conditions for {@link AdvancedOperation AdvancedOperations}
 * <p>
 * Created by kdubb on 2/3/16.
 */
public class Evaluator {

  public static void evaluate(List<Condition> conditions, AdvancedOperation operation,
                              Consumer<Collection<Throwable>> completion) {

    DispatchGroup conditionGroup = new DispatchGroup();

    Vector<Result> results = new Vector<>(conditions.size());
    results.setSize(conditions.size());

    ListIterator<Condition> conditionIterator = conditions.listIterator();

    while(conditionIterator.hasNext()) {

      int index = conditionIterator.nextIndex();
      Condition condition = conditionIterator.next();

      conditionGroup.enter();

      condition.evaluateForOperation(operation, result -> {

        results.set(index, result);

        conditionGroup.leave();

      });

    }

    conditionGroup.setNotification(DispatchQueues.MEDIUM, () -> {

      List<Throwable> failures = FluentIterable.from(results)
          .transform(Result::getError)
          .filter(error -> error != null)
          .toList();

      if(operation.isCancelled()) {
        failures.add(new ConditionFailed());
      }

      completion.accept(failures);

    });

  }

}
