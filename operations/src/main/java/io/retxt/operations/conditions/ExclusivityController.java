package io.retxt.operations.conditions;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import io.retxt.operations.AdvancedOperation;
import io.retxt.operations.AdvancedOperationQueue;
import io.retxt.operations.Operation;

import java.util.Collection;



/**
 * A static to keep track of all the in-flight Operation instances that have declared themselves as requiring mutual
 * exclusivity. We use a singleton because mutual exclusivity must be enforced across the entire app, regardless of the
 * {@link AdvancedOperationQueue} on which an {@link AdvancedOperation} was executed.
 * <p>
 * Created by kdubb on 2/3/16.
 */
public class ExclusivityController {

  private static final ListMultimap<String, Operation> operations = LinkedListMultimap.create();

  public static void addOperation(AdvancedOperation operation, Collection<String> categories) {

    for(String category : categories) {
      addOperation(operation, category);
    }
  }

  public static synchronized void addOperation(AdvancedOperation operation, String category) {

    // This provides the mutual exclusivity. We make the previously added operation a
    // dependency of the newly added operation. This creates a chain of dependencies
    // between all operations of the same categories.
    //
    // Note that ListMultimap keeps the operations in insertion order, which is required
    // for proper dependency chaining

    Operation previouslyAdded = Iterables.getLast(operations.get(category), null);
    if(previouslyAdded != null) {
      operation.addDependency(previouslyAdded);
    }

    operations.put(category, operation);
  }

  public static void removeOperation(AdvancedOperation operation, Collection<String> categories) {

    for(String category : categories) {
      removeOperation(operation, category);
    }
  }

  public synchronized static void removeOperation(AdvancedOperation operation, String category) {

    operations.remove(category, operation);
  }

}
