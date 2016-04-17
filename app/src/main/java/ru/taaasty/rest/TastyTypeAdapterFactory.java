package ru.taaasty.rest;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import ru.taaasty.rest.model.Stats;

/**
 * Created by alexey on 17.04.16.
 */
public class TastyTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (rawType.equals(Stats.class)) {
            return (TypeAdapter<T>) Stats.typeAdapter(gson);
        }
        return null;
    }
}
