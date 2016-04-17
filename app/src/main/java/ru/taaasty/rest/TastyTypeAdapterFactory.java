package ru.taaasty.rest;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import ru.taaasty.rest.model2.Stats;
import ru.taaasty.rest.model2.Userpic;

/**
 * Created by alexey on 17.04.16.
 */
public class TastyTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (rawType.equals(Stats.class)) {
            return (TypeAdapter<T>) Stats.typeAdapter(gson);
        } else if (rawType.equals(Userpic.class)) {
            return (TypeAdapter<T>) Userpic.typeAdapter(gson);
        } else if (rawType.equals(Userpic.DefaultColors.class)) {
            return (TypeAdapter<T>) Userpic.DefaultColors.typeAdapter(gson);
        }
        return null;
    }
}
