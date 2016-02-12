package io.retxt.promise.functions;

import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkState;



public class BiUnion<T1, T2> {

  class TypedValue {

    TypeToken type;
    Object value;

    public TypedValue(TypeToken type, Object value) {
      this.type = type;
      this.value = value;
    }
  }



  private TypedValue current;
  private TypeToken<T1> t1TypeToken;
  private TypeToken<T2> t2TypeToken;

  protected BiUnion() {
    this.t1TypeToken = new TypeToken<T1>(getClass()) {};
    this.t2TypeToken = new TypeToken<T2>(getClass()) {};
  }

  protected boolean is1() {
    return t1TypeToken == current.type;
  }

  @SuppressWarnings("unchecked")
  protected T1 get1() {
    checkState(is1());
    return (T1) t1TypeToken.getRawType().cast(current.value);
  }

  protected void set1(T1 value) {
    this.current = new TypedValue(t1TypeToken, value);
  }

  protected boolean is2() {
    return t2TypeToken == current.type;
  }

  @SuppressWarnings("unchecked")
  protected T2 get2() {
    checkState(is2());
    return (T2) t2TypeToken.getRawType().cast(current.value);
  }

  protected void set2(T2 value) {
    this.current = new TypedValue(t2TypeToken, value);
  }

}



