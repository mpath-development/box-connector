package com.mpath.connector;

import com.google.gson.Gson;
import spark.ResponseTransformer;

/** {@link ResponseTransformer} as shown in the doc to convert gson */
public final class GsonTransformer implements ResponseTransformer {
  private Gson gson = new Gson();

  @Override public String render(Object model) throws Exception {
    return gson.toJson(model);
  }
}
